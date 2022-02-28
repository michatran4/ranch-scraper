# ranch-scraper
This is a scraper that indexes all students that have attended Cypress Ranch
High School, starting from the 2019-2020 school year, and the groups they are in
(along with the email, if they provided one). Is this concerning? Maybe. I don't
know if I need everyone to know what clubs I'm in.

Strictly puppeteer is used as there's no API for public use that allows me to
access CFISD's Schoology.

## Dependencies
- dotenv for providing the username and password.
- puppeteer for scraping.
- cheerio/unirest for scraping the faculty web pages.

## Setup
In `.env`, put your username and password in like the following format:

```
username='username'
password='password'
```

Then, run `npm i` to install modules. You may want to ignore the Chromium
download for puppeteer. Set the environment variable
`PUPPETEER_SKIP_CHROMIUM_DOWNLOAD` equal to true.

## Usage
You will have to log in twice for the scraper to run. A second browser is
instance is used so that the first one with the members tab can add to the queue
constantly (the next button can break on the members tab); the second one will
scrape further.

While indexing id numbers, pages may get stuck. Checks are in place for you to
just click the Previous button. The scraping for the ids should be relatively
quick, so just be attentive until the ids are done.

Scraping the faculty web pages can be done separately with `teachers.js`.

One should determine what clubs should be excluded from the club count, and write it in 
`fake-clubs.json` as a JSON array.

## Analysis with Java
- [org.json](https://github.com/stleary/JSON-java) is used to parse JSON.
The following info can be found:
- the graduation years of each member (though this might not be perfect with some lacking the group)
- who belongs to each graduating class, how many are in each class (inaccurate, people can join other classes)
- who is in the most clubs (excluding announcements and such)
- who is in the most clubs per graduating class
- who is in which clubs, which clubs have the most people

A few students and teachers have private settings on. Therefore, their groups can't be scraped.
However, normal students don't have the ability to set group privacy settings.
This should make group data fairly accurate.
