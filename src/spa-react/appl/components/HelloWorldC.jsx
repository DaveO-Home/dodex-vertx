import React, { Component, useEffect } from "react";
import { createRoot } from 'react-dom/client';
import PropTypes from "prop-types";
import App from "../js/app";
import Setup from "../js/utils/setup";
import "../assets/App.css";
import carousel from "../js/carousel";

class Link extends Component {
  render() {
    const props = {
      url: "https://reactjs.org/",
      text: "React - A JavaScript library for building user interfaces"
    };
    return <a href={props.url} target="_blank" rel="noopener noreferrer">{props.text}</a>;
  }
}

Link.propTypes = {
  url: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired
};

const welcomeHtml = (
  <div id="welcome" className="App">
    {/* <link href="./App.css" rel="stylesheet" /> */}
    <div className="App-header">
      <img className="App-logo" src="../appl/assets/logo.svg" />
      <h2>Hi -
      <span
          className="txt-rotate"
          data-period="2000"
          data-rotate='[ "Acceptance Testing with a React App" ]'>
        </span>
      </h2>
    </div>
    <br />
    <h2>Getting Started</h2>
    <br />
    <ul className="App-intro">
      <li><Link url="" text="" /></li>
    </ul>
  </div>
);

function Carousel() {
    useEffect(() => {
        carousel();
    });
    return welcomeHtml;
}

class Welcome extends Component {
  componentDidMount() {
    setData();
    if (App.controllers["Start"]) {
      App.controllers["Start"].initMenu();
    }
    Setup.init();
  }

  render() {
    return (<span></span>);
  }
}

function setData() {
    if(!window.main) {
        const container = document.getElementById("main_container");
        window.main = createRoot(container);
    }
    window.main.render(
        <Carousel/>
    );
}

export default Welcome;
