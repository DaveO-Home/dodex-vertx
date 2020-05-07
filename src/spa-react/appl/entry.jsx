import React from "react";
import ReactDOM from "react-dom";
import Menulinks, { Dodexlink } from "./Menulinks";
import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";

/* develblock:start */
// if (typeof window.testit === "undefined" || !window.testit) {
if (location.href.indexOf("context.html") === -1) {
/* develblock:end */
  ReactDOM.render(
    <Menulinks />,
    document.getElementById("root")
  );

  ReactDOM.render(
    <Dodexlink />,
    document.querySelector(".dodex--ico")
  );

  if (document.querySelector(".top--dodex") === null) {
    // Content for cards A-Z and static card
    dodex.setContentFile("./dodex/data/content.js");
    dodex.init({
      width: 375,
      height: 200,
      left: "50%",
      top: "100px",
      input: input,    	// required if using frontend content load
      private: "full", 	// frontend load of private content, "none", "full", "partial"(only cards 28-52) - default none
      replace: true,   	// append to or replace default content - default false(append only)
      mess: mess,
      // server: "localhost:3087", // You will have to start the node server for port 3087 - do the following;
      // cd to src/main/resources/static/node_modules/dodex-mess/server and execute "npm install" then "node koa" 
      // server: "daveomix.us-south.cf.appdomain.cloud" // This will link to the cloud version
      // for the verticle "dodex-vertx" use
      // server: "localhost:8087" // if the test verticle is running.
      server: "localhost:8087"
    }).then(function () {
      // Add in app/personal cards
      for (let i = 0;i < 4;i++) {
        dodex.addCard(getAdditionalContent());
      }
      /* Auto display of widget */
      // dodex.openDodex();
    });
  }
/* develblock:start */
}
/* develblock:end */

function getAdditionalContent() {
  return {
    cards: {
      card28: {
        tab: "F01999", // Only first 3 characters will show on the tab.
        front: {
          content: `<h1 style="font-size: 10px;">Friends</h1>
        <address style="width:385px">
          <strong>Charlie Brown</strong> 	111 Ace Ave. Pet Town
          <abbr title="phone"> : </abbr>555 555-1212<br>
          <abbr title="email" class="mr-1"></abbr><a href="mailto:cbrown@pets.com">cbrown@pets.com</a>
        </address>
        `
        },
        back: {
          content: `<h1 style="font-size: 10px;">More Friends</h1>
        <address style="width:385px">
          <strong>Lucy</strong> 113 Ace Ave. Pet Town
          <abbr title="phone"> : </abbr>555 555-1255<br>
          <abbr title="email" class="mr-1"></abbr><a href="mailto:lucy@pets.com">lucy@pets.com</a>
        </address>
        `
        }
      },
      card29: {
        tab: "F02",
        front: {
          content: "<h1 style=\"font-size: 14px;\">My New Card Front</h1>"
        },
        back: {
          content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
        }
      },
			card30: {
				tab: "NP",
				front: {
					content: "<a class=\"twitter-timeline\" data-width=\"340\" data-height=\"200\" href=\"https://twitter.com/TwitterDev/lists/national-parks?ref_src=twsrc%5Etfw\">A Twitter List by TwitterDev</a>"
				},
				back: {
					content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
				}
			},
			card31: {
				tab: "LIK",
				front: {
					content: '<a class="twitter-timeline" data-width="340" data-height="200" href="https://twitter.com/TwitterDev/likes?ref_src=twsrc%5Etfw">Tweets Liked by @TwitterDev</a>'
				},
				back: {
					content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
				}
			}
    }
  };
}
