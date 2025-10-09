import ToolsSM from "../js/utils/tools.sm";
import popper from "@popperjs/core";
import { act } from 'react';

export default function (Tools, Helpers, React, timer) {
    /*
     * Test that new data are loaded when the select value changes.
     */
    describe("Load new tools page", function () {
        let tools;
        let beforeValue;
        let afterValue;
        let spyToolsEvent;
        let selectorObject;
        let selectorItem;
        let defaultReduxValue;
        let newReduxValue;

        beforeAll(async() => {
            $("#dropdown1").remove();
            if(!window.main) {
                const container = document.getElementById("main_container");
                window.main = createRoot(container);
            }
            await act(async() => {
                await window.main.render(
                    <Tools />
                );
            })

            tools = $("#tools");
            beforeValue = tools.find("tbody").find("tr:nth-child(1)").find("td:nth-child(2)").text();
            defaultReduxValue = $("#tools-state").text().split(" ", 1);

            selectorObject = $("#dropdown0");
            selectorObject = document.activeElement;
            await act(async() => {
                await selectorObject.click();
            });
            selectorItem = $("#dropdown1 a")[1];
//                    spyToolsEvent = spyOnEvent(selectorItem, "select");
            await act(async() => {
                await selectorItem.click();
            })
            Helpers.fireEvent(selectorItem, "select");
        }, 3000);

        it("setup and click events executed.", done =>  {
            const numbers = timer(100, 50);
            const observable = numbers.subscribe(timer => {
                afterValue = tools.find("tbody").find("tr:nth-child(1)").find("td:nth-child(2)").text();
                if (afterValue !== beforeValue || timer === 100) {
                    newReduxValue = $("#tools-state").text().split(" ", 1);
                    observable.unsubscribe();
                    done();
                }
            });
            // jasmine-jquery matchers - no longer work with karma-jasmine/karma-jquery
//            expect("select").toHaveBeenTriggeredOn(selectorItem);
//            expect(spyToolsEvent).toHaveBeenTriggered();

            expect(tools[0]).toBeInDOM();
            expect(".disabled").toBeDisabled();
            expect("#dropdown1 a").toHaveLength(3);
            // Required for Firefox
            selectorObject[0] = document.activeElement;
            expect(selectorObject).toBeFocused();
        });

        it("did Redux set default value.", () => {
            expect(defaultReduxValue[0]).toBe("Combined");
        });

        it("new page loaded on change.", () =>{
            expect(beforeValue).not.toBe(afterValue);
        });

        it("did Redux set new value.", () => {
            expect(newReduxValue[0]).toBe("Category1");
        });

        it("verify state management", () => {
            const items = ToolsSM.getStore().getState().tools.items;
            expect(items.length).toBe(2);
            expect(items[0].displayed).toBe(false);
            expect(items[1].displayed).toBe(true);
            expect(items[1].message).toBe("Category1");
        });
    });
}
