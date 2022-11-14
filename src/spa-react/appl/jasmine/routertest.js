// Note: Menulinks was loaded in entry.js
import { HashRouter } from "react-router-dom";
import { createMemoryHistory } from 'history'
import { render } from '@testing-library/react'
import React from 'react'
import Pdf from "../components/PdfC";
import Tools from "../components/ToolsC";
import ErrorBoundary from "../components/ErrorBoundary";

export default function (type) {
    if (window.testit !== undefined && window.testit) {
        describe("Testing Menulinks Router", () => {
            const history = createMemoryHistory();
            let route = "";
            it(`is ${type} loaded from router component`, (done) => {
                switch (type) {
                    case "table":
                        route = "#/table/tools";
                        history.push(route)
                        render(
                            <HashRouter history={history}>
                                <ErrorBoundary>
                                    <Tools />
                                </ErrorBoundary>
                            </HashRouter>
                        )
                        setTimeout(function () {
                            expect($("tbody > tr[role=\"row\"]").length > 65).toBe(true);  // default page size
                            done();
                        }, 750);
                        break;
                    case "pdf":
                        route = '#/pdf/test'
                        history.push(route)
                        render(
                            <HashRouter history={history}>
                                <ErrorBoundary>
                                    <Pdf />
                                </ErrorBoundary>
                            </HashRouter>
                        )
                        expect($("#main_container > iframe[name=\"pdfDO\"]").length > 0).toBe(true);
                        done();
                        break;
                    default:
                }
            });
        });
    }
}
