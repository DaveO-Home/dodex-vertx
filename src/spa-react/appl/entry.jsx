import React from "react";
import { createRoot } from 'react-dom/client';
import Menulinks, { Dodexlink } from "./Menulinks";
import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";

/* develblock:start */
if (location.href.indexOf("context.html") === -1) {
/* develblock:end */
    if(!window.rootRoot) {
        const container = document.getElementById("root");
        window.rootRoot = createRoot(container);
    }
    window.rootRoot.render(
        <Menulinks />
    );

    if(!window.dodexRoot) {
        const dodexContainer = document.querySelector(".dodex--ico");
        window.dodexRoot = createRoot(dodexContainer);
    }
    window.dodexRoot.render(
        <Dodexlink />
    );

  if (document.querySelector(".top--dodex") === null) {
    // this should handle all hostnames and ports for the websocket setup
    const server = window.location.hostname + (window.location.port.length > 0 ? ":" + window.location.port : "");
    // Content for cards A-Z and static card
    dodex.setContentFile("./dodex/data/content.js");
    setTimeout(function() {
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
          // server: "localhost:8089" // if the test verticle is running.
          server: server
        }).then(function () {
          // Add in app/personal cards
          for (let i = 0;i < 4; i++) {
            dodex.addCard(getAdditionalContent());
          }
          /* Auto display of widget */
//           dodex.openDodex();
        });
      }, 500);
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
        tab: "TWT",
        front: {
          content: '<blockquote data-cards="hidden" class="twitter-tweet"><p lang="en" dir="ltr">Sunsets don&#39;t get much better than this one over <a href="https://twitter.com/GrandTetonNPS?ref_src=twsrc%5Etfw">@GrandTetonNPS</a>. <a href="https://twitter.com/hashtag/nature?src=hash&amp;ref_src=twsrc%5Etfw">#nature</a> <a href="https://twitter.com/hashtag/sunset?src=hash&amp;ref_src=twsrc%5Etfw">#sunset</a> <a href="http://t.co/YuKy2rcjyU">pic.twitter.com/YuKy2rcjyU</a></p>&mdash; US Department of the Interior (@Interior) </blockquote>'
        },
        back: {
          content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
        }
      }
    } // <a href="https://twitter.com/Interior/status/463440424141459456?ref_src=twsrc%5Etfw">May 5, 2014</a>
  };
}
