import React from "react";
import ReactDOM from "react-dom";
import Menulinks, { Dodexlink } from "./Menulinks";
import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";

let port = 8080; // Must equal Vertx production port 
/* develblock:start */
// if (typeof window.testit === "undefined" || !window.testit) {
if (location.href.indexOf("context.html") === -1) {
  port = 8087;
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
      height: 210,
      left: "50%",
      top: "100px",
      input: input,    	// required if using frontend content load
      private: "full", 	// frontend load of private content, "none", "full", "partial"(only cards 28-52) - default none
      replace: true,   	// append to or replace default content - default false(append only)
      mess: mess,
      server: "localhost:" + port
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
        tab: "TW",
        front: {
          content: '<blockquote data-cards="hidden" class="twitter-tweet"><p lang="en" dir="ltr">Sunsets don&#39;t get much better than this one over <a href="https://twitter.com/GrandTetonNPS?ref_src=twsrc%5Etfw">@GrandTetonNPS</a>. <a href="https://twitter.com/hashtag/nature?src=hash&amp;ref_src=twsrc%5Etfw">#nature</a> <a href="https://twitter.com/hashtag/sunset?src=hash&amp;ref_src=twsrc%5Etfw">#sunset</a> <a href="http://t.co/YuKy2rcjyU">pic.twitter.com/YuKy2rcjyU</a></p>&mdash; US Department of the Interior (@Interior) <a href="https://twitter.com/Interior/status/463440424141459456?ref_src=twsrc%5Etfw">May 5, 2014</a></blockquote>'
        },
        back: {
          content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
        }
      }
    }
  };
}
