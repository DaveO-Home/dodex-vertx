import React, { Component } from "react";
import { messageAlert } from "../js/utils/alert";

class Dodex extends Component {
  render() {
    return (
      <div className="dodex--open" onClick={this.handleClick}>
        <img src="../images/dodex_g.ico" />
      </div>
    );
  }

  handleClick() {
    const credentials = sessionStorage.getItem("credentials");
    if (typeof $("#top-nav").find(".alert-warning")[0] !== "undefined") {
      window.doDex.openDodex();
      return;
    }
    if (!credentials) {
      messageAlert($("#top-nav"), "<strong>Warning:</strong> You need to login for Dodex", "warning");
      window.doDex.openDodex(); // toggle close
      return;
    }
  }
}

export default Dodex;
