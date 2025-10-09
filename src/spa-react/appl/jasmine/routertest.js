// Note: Menulinks was loaded in entry.js
import { render, screen, act } from "@testing-library/react";
import React from "react";
import Tools, { getToolsComp } from "../components/ToolsC";
import Pdf from "../components/PdfC";
export default function (type, timer) {
    if (window.testit !== undefined && window.testit) {
        describe("Testing Menulinks Router", () => {
            it(`is ${type} loaded from router component`, async() => {
                switch (type) {
                    case "table":
                        await act(async() => {
                            await render(<Tools/>);
                        });

//                        await act(async() => {
//                            await screen.getAllByText("Tabular View")[0].click();
//                        });

                       const numbers = timer(50, 50);
                       const observable = numbers.subscribe(timer => {
                           if ($('tbody > tr[role="row"]').length != 0 || timer === 25) {
                               expect($('tbody > tr[role="row"]').length > 65).toBe(true);  // default page size
                               observable.unsubscribe();
                           }
                       });
                        break;
                    case "pdf":
                        await act(async() => {
                            await render(<Pdf/>);
                        })
                        act(() => {
                            render(<Pdf/>);
                        })
                        expect($('#main_container > iframe[name="pdfDO"]').length > 0).toBe(true);
                        break;
                    default:
                }
            });
        });
    }
}
