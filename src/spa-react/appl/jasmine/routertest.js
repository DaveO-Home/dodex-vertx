// Note: Menulinks was loaded in entry.js
import { render, screen, act } from "@testing-library/react";
import React from "react";
import Tools, { getToolsComp } from "../components/ToolsC";
import Pdf from "../components/PdfC";
export default function (type, timer) {
    if (window.testit !== undefined && window.testit) {
        describe("Testing Menulinks Router", () => {
            it(`is ${type} loaded from router component`, done => {
                switch (type) {
                    case "table":
                        act(() => {
                            render(<Tools/>);
                        });

                        act(() => {
                            screen.getAllByText("Tabular View")[0].click();
                        });

                       const numbers = timer(50, 50);
                       const observable = numbers.subscribe(timer => {
                           if ($('tbody > tr[role="row"]').length != 0 || timer === 25) {
                               expect($('tbody > tr[role="row"]').length > 65).toBe(true);  // default page size
                               observable.unsubscribe();
                               done();
                           }
                       });
                        break;
                    case "pdf":
                        render(<Pdf/>);
                        act(() => {
                            screen.getAllByText("PDF View")[0].click();
                        })
                        expect($('#main_container > iframe[name="pdfDO"]').length > 0).toBe(true);
                        done();
                        break;
                    default:
                }
            });
        });
    }
}
