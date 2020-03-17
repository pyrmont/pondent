const fs = require("fs")
const jsdom = require("jsdom")
const fetch = require("node-fetch")
const repl = require("repl")

const url = process.argv[2]
const load = async url => {
  try {
    const response = await fetch(url)
    const html = await response.text()

    const { JSDOM } = jsdom
    const { window } = new JSDOM(html, {
      url: url,
      runScripts: "dangerously",
      resources: "usable",
      pretendToBeVisual: true })

     repl.start()
   } catch (error) {
     console.log(error)
   }
};

load(url);
