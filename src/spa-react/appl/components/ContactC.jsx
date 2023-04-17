import React, { Component } from "react";
import App from "../js/app";
import Setup from "../js/utils/setup";
import Helpers from "../js/utils/helpers";

const contactHtml = (
    <span id="data" name="contact">
        <div className="d-flex justify-content-center">
            <div className="p-2 z-2100">
                <div className="page-header">
                    <h2>Contact <small>US</small></h2>
                </div>

                <address className="pull-left" style={{ width: "300px" }}>
                    <strong>The Best Company</strong><br />
                    111 Swell Ave. Suite 105<br />
                    Middle, Earth 11111
            </address>

                <address className="pull-right">
                    <abbr title="Phone" className="mr-2">Phone:</abbr>800 555-5555<br />
                    <abbr title="Phone" className="mr-3">Local:</abbr>517 555-1212<br />
                    <abbr title="Phone" className="mr-4">Fax: </abbr> 800 555-1212<br />
                    <abbr title="Phone" className="mr-3">Email:</abbr><a href="mailto:info@best.com">info@best.com</a>
                </address>

                <div className="clearfix"></div>

                <form action="#" method="post" className="form-horizontal">

                    <fieldset>
                        <legend>Via Email:</legend>

                        <div className="control-group mt-2 required">
                            <div className="control-label">Name<span className="text-danger">*</span></div>
                            <div className="controls">
                                <div className="input-prepend">
                                    <input type="text" name="name" id="inputName" placeholder="Name" required />
                                </div>
                            </div>
                        </div>

                        <div className="control-group mt-2 required">
                            <div className="control-label">Email</div>
                            <div className="controls">
                                <div className="input-prepend">
                                    <input id="inputEmail" name="email" type="email" placeholder="Email" required />
                                </div>
                            </div>
                        </div>

                        <div className="control-group">
                            <div className="control-label mt-2">Reason</div>
                            <div className="controls">
                                <select id="inputReason" name="reason">
                                    <option value="general">General</option>
                                    <option value="delivery">Delivery</option>
                                    <option value="returns">Returns</option>
                                    <option value="quality">Quality</option>
                                </select>
                            </div>
                        </div>
                        <div className="control-group mt-2 required">
                            <div className="control-label">Description</div>
                            <div className="controls">
                                <textarea name="comment" id="inputComment" placeholder="Description" required></textarea>
                            </div>
                        </div>
                        <div className="control-group">
                            <div className="controls">
                                <div className="checkbox">
                                    <input name="only" type="checkbox" /> No Reply Required
                                </div>
                            </div>
                        </div>
                        <div className="control-group mt-2">
                            <div className="controls contact-submit">
                                <input type="submit" value="Submit" className="btn" />
                            </div>
                        </div>
                    </fieldset>
                </form>
            </div>
            <div style={{ width: "150px" }}> </div>
        </div>
    </span>);

class Contact extends Component {
    componentDidMount() {
        setData();
        setTimeout(function () {  // Next tick
            const el = $("#data");
            initStart(el);
        }, 1);
    }

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

function setData() {
    if(window.main) {
        window.main.render(
            contactHtml
        );
    }
}

function getContact() {
    setData();
    return new Promise((resolve, reject) => {
        Helpers.isResolved(resolve, reject, null, "main_container", 0, 0);
    }).catch(rejected => {
        fail(`Contact Page did not load within limited time: ${rejected}`);
    }).then(() => {
        const el = $("#data");
        initStart(el);
        return this;
    });
}

export { getContact };

export default Contact;

const initStart = (el) => {
    const controllerName = "Start";
    const actionName = "init";
    const failMsg = `Load problem with: '${controllerName}/${actionName}'.`;

    App.loadController(controllerName, {}, controller => {
        if (controller &&
            controller[actionName]) {
            controller.initMenu();
            controller.contactListener(el, controller);
        } else {
            console.error(failMsg);
        }
    }, err => {
        console.error(`${failMsg} - ${err}`);
    });
};
