const unirest = require('unirest');
const cheerio = require('cheerio');
const fs = require('fs');

// Scrape teachers from the faculty web page so that the index can be compared against the list.
// This will allow for a higher chance for determining if someone is a teacher.
(async()=>{
    let index = [];
	await unirest.get('https://app02.cfisd.net/urlcap/campus_list_012.html').then(function(result) {
		let $ = cheerio.load(result.body);
		$('body > center > center > table > tbody > tr').find('a').each(function() { // find emails
            // previous node is the full name of the teacher
            let name = $(this.prev).text();
            name = name.replaceAll('\n', ''); // stupid formatting
            name = name.substring(0, name.length - 1);
            // make it a regular name to save to an index
            names = name.split(", ");
            index.push(names[1] + " " + names[0]);
        });
	});
    fs.writeFile('teachers.json', JSON.stringify(index), function (err) {
        if (err) return console.log(err);
        console.log('Written the file.');
    })
})();

