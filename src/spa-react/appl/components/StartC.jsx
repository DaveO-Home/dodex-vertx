import React, { Component } from "react";
import { createRoot } from 'react-dom/client';
import start from "../js/controller/start";
import Setup from "../js/utils/setup";
import Helpers from "../js/utils/helpers";

class Start extends React.Component {
  componentDidMount() {
    getStartComp().then(function(StartComp){
        if(!window.main) {
            const container = document.getElementById("main_container");
            window.main = createRoot(container);
        }
        window.main.render(
            <StartComp />
        );
    });   
  }

  render() {
    return (<span></span>);
  }
}

function getStartComp() {
  start.initMenu();
  start.index();

  return new Promise(function (resolve, reject) {
    let count = 0;
    Helpers.isLoaded(resolve, reject, {}, start, count, 10);
  })
    .catch(function (rejected) {
      console.warn("Failed", rejected);
    })
    .then(function (resolved) {
      const innerHtml = { __html: resolved };
      Setup.init();

      class Start extends Component {
        render() {
          return (
            <span dangerouslySetInnerHTML={innerHtml} />
          );
        }
      }

      return Start;
    });
}

export { getStartComp };

export default Start;
