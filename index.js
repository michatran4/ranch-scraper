// Scrapes the entirety of Cy-Ranch by accessing the Announcements group.
// Also scrapes all groups that all users are present in.
// Don't make puppeteer headless; make sure that the initial scrape isn't stuck.

const url = 'https://cfisd.schoology.com/group/5277638093/members'; // announcements group url
const puppeteer = require("puppeteer");
require('dotenv').config();
const fs = require('fs');

let index = {};

const queue = []; // queue with member ids to further scrape
const wait_time = 800; // 800 ms between each member id in the queue
// 800 is tested to work, 667 is the bare minimum and is not stable

const isNextEnabled = async (page) => { // if there are more members in the group
    return await page.evaluate(() => {
        if (document.getElementsByClassName('next sEnrollmentEditprocessed')[0].classList[1] == 'disabled') {
            return false;
        }
        return true;
    });
}

/**
 * Scrape ids from the announcements page.
 */
async function scrape() {
    const browser = await puppeteer.launch({
        executablePath: "/usr/bin/chromium",
        headless: false,
    });
    const page = await browser.newPage();
    // sign in process first
    await page.goto("https://my.cfisd.net", { waitUntil: 'domcontentloaded' });

    await page.waitForSelector('#username');
    await page.type('#username', process.env.username);
    await page.waitForSelector('#password');
    await page.type('#password', process.env.password);
    await new Promise(resolve => setTimeout(resolve, 500)); // waiting doesn't work properly
    await page.click('#signin');
    console.log("Clicked sign in.");
    await page.waitForNavigation();
    await page.goto(url); // navigate to the page with all of the members

    index = await page.evaluate(() => { // get one index before looping (not clicking the next button)
        // grab all member names and ids (to access later) from the first page
        let page_index = {}
        const members = document.getElementsByClassName('enrollment-user-list')[0].children[0].tBodies[0].children;
        for (let i = 0; i < members.length; i++) {
            let member = {};
            let name = members[i].children[1].children[0].innerHTML;
            name = name.replaceAll('<b>', '').replaceAll('</b>', '').replaceAll('amp;', '');
            member["name"] = name;
            /*
            let badges = members[i].children[1].children.length;
            member["status"] = (badges > 1 ? "ADMIN" : "MEMBER");
            */
            page_index[members[i].id] = member; // key: id, value: member with name property

        }
        return page_index;
    })

    for (let member_id in index) {
        queue.push(member_id);
    }
    startQueueScrape();

    let loadMoreVisible = await isNextEnabled(page); // for use with groups that may only have 1 page (just in case)
    while (loadMoreVisible) {
        await page.click('#roster-wrapper > div > div > div > div.enrollment-view-nav > div.next.sEnrollmentEditprocessed');
        while (true) { // hackery with ajax
            try {
                // move to the next page
                let content = await page.evaluate(() => {
                    // grab all member names and ids from the current page
                    let page_index = {};
                    const members = document.getElementsByClassName('enrollment-user-list')[0].children[0].tBodies[0].children;
                    for (let i = 0; i < members.length; i++) {
                        let member = {};
                        let name = members[i].children[1].children[0].innerHTML;
                        name = name.replaceAll('<b>', '').replaceAll('</b>', '').replaceAll('amp;', '');
                        member["name"] = name;
                        /*
                        let badges = members[i].children[1].children.length;
                        member["status"] = (badges > 1 ? "ADMIN" : "MEMBER");
                        */
                        page_index[members[i].id] = member; // key: id, value: member with name property
                    }
                    return page_index;
                });
                for (let member_id in content) {
                    if (!(member_id in index)) {
                        index[member_id] = content[member_id]; // add the content json the current index json
                        queue.push(member_id);
                    }
                }
                loadMoreVisible = await isNextEnabled(page); // check if there's more pages
                break;
            } 
            catch(e){}
        }
    }
    console.log('Initial scraping is finished. Final: ' + Object.keys(index).length);
}

function isInt(value) {
    return !isNaN(value) && parseInt(Number(value)) == value;
}

/**
 * Scrape the groups and email of a user id.
 * @param {string} id 
 * @param {puppeteer.Page} page 
 */
async function scrapeGroupsAndEmail(id, page) {
    await page.goto(`https://cfisd.schoology.com/user/${id}/groups/list`, {waitUntil: 'load'}); // access groups list page for the groups
    while (true) {
        try {
            let groups = await page.evaluate(() => {
                if (document.getElementById('error-page') != null) { // private access
                    return "null " + document.getElementById('error-page').innerHTML;
                }
                let map = {};
                let list = document.getElementsByClassName('my-groups-item-list')[0].children; // groups
                for (let i = 0; i < list.length; i++) {
                    let group = list[i].children[0].children[1].children[0];
                    if (group.href == undefined) { // some don't have links. just store their group name.
                        map[group.innerHTML] = group.innerHTML;
                    }
                    else { // store the group id
                        let id = group.href.substring(group.href.lastIndexOf('/') + 1);
                        let name = group.innerHTML;
                        map[id] = name;
                    }
                }
                return map;
            })
            if (typeof groups == 'string') {
                console.log("ERROR: " + groups);
                break;
            }
            index[`${id}`]["groups"] = Object.keys(groups); // log the groups the user belongs to
            filter = {}
            Object.keys(groups).forEach(function(key) { // check for groups that were names and not just ids
                if (isInt(key)) {
                    filter[key] = groups[key];
                }
            })
            index["group_id"] = Object.assign({}, index["group_id"], filter); // update groups dictionary (only integers)
            break;
        }
        catch(e){}
    }
    await page.goto(`https://cfisd.schoology.com/user/${id}/info`, {waitUntil: 'load'}); // access info page for the email
    while (true) {
        try {
            let email = await page.evaluate(() => {
                if (document.getElementById('error-page') != null) { // private access, just in case
                    return "";
                }
                if (document.getElementsByClassName('admin-val email').length > 0) { // scrape the email
                    const mailto = document.getElementsByClassName('admin-val email')[0].children[0].href;
                    return mailto.substring(mailto.lastIndexOf(':') + 1);
                }
                return "";
            })
            if (email != "") { // some don't provide an email
                index[`${id}`]["email"] = email;
            }
            break;
        }
        catch(e){}
    }
    page.close();
}

/**
 * Start scraping member ids further, using a queue due to rate limits.
 */
async function startQueueScrape() {
    const new_browser = await puppeteer.launch({
        executablePath: "/usr/bin/chromium",
        headless: false,
    });
    const page = await new_browser.newPage();
    // sign in process first
    await page.goto("https://my.cfisd.net", { waitUntil: 'domcontentloaded' });

    await page.waitForSelector('#username');
    await page.type('#username', process.env.username);
    await page.waitForSelector('#password');
    await page.type('#password', process.env.password);
    await new Promise(resolve => setTimeout(resolve, 500)); // waiting doesn't work properly
    await page.click('#signin');
    console.log("Clicked sign in.");
    await page.waitForNavigation();
    while (queue.length != 0) { // while there are still elements being added or there are current elements, check
        if (queue.length > 0) { // OR condition, so just make sure there are elements to dequeue
            let member_id = queue.shift();
            let member_page = await new_browser.newPage();
            scrapeGroupsAndEmail(member_id, member_page);
            await new Promise(resolve => setTimeout(resolve, wait_time)); // requests do not need to be kept track of for ajax.
            // don't cut it close though
        }
    }
    fs.writeFile('index.json', JSON.stringify(index), function (err) {
        if (err) return console.log(err);
        console.log('Written the file.');
    })
}

/**
 * Cause schoology to rate limit and display the error message for debugging.
 */
/*
async function rateLimit() {
    const browser = await puppeteer.launch({
        executablePath: "/usr/bin/chromium",
        headless: false,
    });
    const page = await browser.newPage();
    // sign in process first
    await page.goto("https://my.cfisd.net", { waitUntil: 'domcontentloaded' });

    await page.waitForSelector('#username');
    await page.type('#username', process.env.username);
    await page.waitForSelector('#password');
    await page.type('#password', process.env.password);
    //await page.click('#signin'); // automatic sign in is not fully there :(
    await page.waitForNavigation();
    for (let i = 0; i < 15; i++) {
        const bruh = await browser.newPage();
        bruh.goto(url);
    }
}
*/

scrape();
