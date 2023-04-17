import React, { Component } from "react";
import { createRoot } from 'react-dom/client';
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
    return (<span></span>);
  }
}

function getPdfComp() {
  return pdfIframe;
}

function setPdfComp() {
    window.main.render(
        pdfIframe
    );
}

export { getPdfComp };

export default Pdf;
