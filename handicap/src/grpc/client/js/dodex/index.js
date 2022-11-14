import dodex from "dodex";
import input from "dodex-input";
import mess from "dodex-mess";
import "dodex/dist/dodex.min.css";
import "jsoneditor/dist/jsoneditor.min.css"
import jsonEditor from "jsoneditor"
import "@fortawesome/fontawesome-free/js/all.js";
import "@fortawesome/fontawesome-free/js/fontawesome.js";
window.JSONEditor = jsonEditor;

if (document.querySelector(".top--dodex") === null) {
    // Content for cards A-Z and static card
    dodex.setContentFile("content.js");
    // const server = window.location.hostname + (window.location.port.length > 0 ? ":" + window.location.port : "");
    const server = "localhost:8087"; // by default "cors" is set for dev, a router change is requried for prod
    dodex.init({
      width: 375,
      height: 200,
      left: "50%",
      top: "100px",
      input: input,
      private: "full",
      replace: true,
      mess: mess,
      server: server
    }).then(function () {
      // Add in app/personal cards
      for (let i = 0; i < 4; i++) {
        dodex.addCard(getAdditionalContent());
      }
      /* Auto display of widget */
      // dodex.openDodex();
    });
  }
  
  function getAdditionalContent() {
    return {
      cards: {
        card28: {
          tab: "F01", // Only first 3 characters will show on the tab.
          front: {
            content: ``
          },
          back: {
            content: ``
          }
        },
        card29: {
          tab: "F02",
          front: {
            content: ""
          },
          back: {
            content: ""
          }
        },
        card30: {
          tab: "NP",
          front: {
            content: ""
          },
          back: {
            content: ""
          }
        },
        card31: {
          tab: "TW",
          front: {
            content: ''
          },
          back: {
            content: ""
          }
        }
      }
    };
  }
  