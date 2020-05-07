import React, { Component } from "react";
import ReactDOM from "react-dom";
import Setup from "../js/utils/setup";
import App from "../js/app";

App.init();
const url = "views/prod/Test.pdf";

const pdfIframe = (
  <iframe id="data" name="pdfDO" src={url} className="col-lg-12" style={{ height: "750px" }}><span/></iframe>
);

class Pdf extends Component {
  componentDidMount() { setPdfComp(); }
  render() {
    {
      if (App.controllers["Start"]) {
        App.controllers["Start"].initMenu();
      }
      Setup.init();
    }
    return (<span></span>);
  }
}

function getPdfComp() {
  return pdfIframe;
}

function setPdfComp() {
  ReactDOM.render(
    pdfIframe,
    document.getElementById("main_container")
  );
}

export { getPdfComp };

export default Pdf;
