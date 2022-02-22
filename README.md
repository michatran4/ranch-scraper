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
