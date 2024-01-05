const year = new Date().getFullYear();
 var dodexContent = {
	cards: {
		card1: {
			tab: "A",
			front: {
				content: `<span class="handicap-title text-center"><div> Administration</div></span
				<br>
				<pre><strong>Alert: </strong>
    If the <strong>Add Tee</strong> button is disabled, please send
    the course\'s <strong>score card</strong> to the <strong>dodex</strong> private
    message handle <strong>daveo</strong>(ctrl+double-click on a dial)
				</pre>
				`
			},
			back: {
				content: ""
			}
		},
		card3: {
			tab: "C",
			front: {
				content: `
				<ul>
                    <li>Add a course by entering it\'s name with one radio button selected for the tee.</li>
                    <li>You can also change the tee\'s color.</li>
                    <li>The <strong>rating</strong>, <strong>slope</strong> and <strong>par</strong> values are also required.</li>
                    <li>Click the <Strong>Add Tee</strong> button.</li>
                    <li>After the first added tee, the others can be added when needed.</li>
                    <li>When re-logging in and selecting a previously added course the settings for the default tee(white) may not show.</li>
                    <span class="handicap-title">Continue on back(click "C" tab)...</span>
                </ul>
				`
			},
			back: {
				content: `<span> - Course</span>
                <ul>
                    <li>Simply select another radio button and then the default radio button.</li>
                    <li>A golfer cannot add a <strong>Course</strong> if the administrator has disabled the capability.</li>
                    <li>The golfer will have to notify the administrator with a <strong>score card</strong>.
                </ul
				`
			}
		},
		card4: {
			tab: "D",
			front: {
				content: `
				<ul>
                    <li>Clicking on the dodex icon will toggle the widget\'s visibility.</li>
                    <li>Click a tab to page to adjacent card.</li>
                    <li>Click the face or back of a card to flip current cards.</li>
                    <li>Enter a dial with mouse and with mouse down slowly move up or down to flip cards.</li>
                    <li>Double-Click on bottom static card or dials to popup the front-end load file form.</li>
                    <li>Double-Click again to close or click close button.</li>
                    <span class="handicap-title">Continue on back(click "D" tab)...</span>
                </ul>
                `
			},
			back: {
				content: `<span> - Dodex</span>
				<ul>
				    <li>Ctrl+Double-Click on bottom static card or dials to popup the client messaging form.</li>
                    <li>On initial access, the <strong>User Id</strong> input form will show. Enter your handle for messaging.</li>
                    <li>Click on <strong>more button</strong> to view options.</li>
                    <li>To broadcast, enter a message and click "send".</li>
                    <li>Messaging to one or more golfing buddies, select on the private message drop down after clicking the <strong>more button</strong>.</li>
                </ul>
                `

			}
		},
        card7: {
            tab: "G",
            front: {
                content: `
                    <ul>
                        <li>First time login simply enter a pin(2 alpha characters with between 4-6 addition alpha-numeric characters) with first and last name.</li>
                        <li>Click the login button to create/login to the application.</li>
                        <li>On subsequent logins only the pin is needed, don\'t forget it.</li>
                        <li>The Country and State should also be selected before creating a pin as default values.</li>
                        <li>However, you can change the defaults on any login.</li>
                    </ul>
                    <span class="handicap-title">Continue on back(click "G" tab)...</span>
                    `
            },
            back: {
                content: `<span> - Golfer</span>
                    <ul>
                        <li>Also, Overlap Years and Public should be set.</li>
                         <li>Overlap will use the previous year\'s scores for the handicap if needed.</li>
                         <li>Public will allow your scores to show up in the Scores tab for public viewing.</li>
                     </li>
                    `
            }
        },
		card12: {
			tab: "L",
			front: {
			    content: `
			    <br>
                <ol>
                    <li><a class="link" href="https://github.com/DaveO-Home/dodex-vertx/tree/master/handicap/" target="_">Handicap Read-Me</a></li>
                    <li><a class="link" href="https://github.com/DaveO-Home/dodex/" target="_">Dodex Read-Me</a></li>
                    <li><a class="link" href="https://github.com/DaveO-Home/dodex-mess/" target="_">Dodex Messaging Read-Me</a></li>
                    <li><a class="link" href="https://github.com/DaveO-Home/dodex-input/" target="_">Dodex Input Read-Me</a></li>
                <ol>
			`
			},
			back: {
			    content: ``
			}
		},
		card19: {
			tab: "S",
			front: {
				content: `
				<ul>
                    <li>To add a score, select previously added <strong>course</strong> and <strong>tee</strong> with values for <strong>Gross Score</strong>, <strong>Adjusted Score</strong> and <strong>Tee Time</strong>.</li>
                    <li>Click the <strong>Add Score</strong> button to add the score.</li>
                    <li>The <strong>Remove Last Score</strong> will remove the last entered score, multiple clicks will remove multiple scores.</li>
				</ul>
				<strong>Note:</strong> A handicap will be generated after 5 scores have been added.
				`
			},
			back: {
				content: `<span> - Score</span>

				`
			}
		},
		card27: {
			tab: "",
			front: {
			  content: ""
			},
			back: {
			  content: `<span class="handicap handicap-title text-center"><div> Welcome to Dodex Handicap</div></span>
                <div class="handicap green handicap-table handicap-title text-center pt-2 w-100 h-100">
                    <div class="pb-2">Instructions</div>
                    <ol start="1" class="text-start">
                        <li>
                            <span class="tab-g"><a href="#">Tab G:</a> Adding Golfer</span>
                        </li>
                        <li>
                            <span class="tab-c"><a href="#">Tab C:</a> Adding Course and Tees</span>
                        </li>
                        <li>
                            <span class="tab-s"><a href="#">Tab S:</a> Entering and Removing a Score</span>
                        </li>
                        <li>
                            <span class="tab-d"><a href="#">Tab D:</a> Using Dodex</span>
                        </li>
                        <li>
                            <span class="tab-l"><a href="#">Tab L:</a> Links to Dodex Information</span>
                        </li>
                    </ol>
                </div>

			  <table class=""  style="">
			    <tbody class="">
			    <row class="">
				<td>
				</tbody></table>

				  <footer class="footer back27-handicap" style="width:96%;">
					    <div class="footer-col col-sm-10">
                            <ul class="list-inline" style="width:120%;">
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.pgatour.com/" target="_"><i class="fa-solid fa-golf-ball-tee"></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.facebook.com/" target="_"><i class="fa-brands fa-facebook"></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://news.google.com/" target="_"><i class="fa-brands fa-google"></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://twitter.com/Twitter" target="_"><i class="fa-brands fa-twitter"></i></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.linkedin.com/" target="_"><i class="fa-brands fa-linkedin"></i></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://dribbble.com/" target="_"><i class="fa-brands fa-dribbble"></i></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.reddit.com/" target="_"><i class="fa fa-brands fa-reddit"></i></a></li>
                                <li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.tiktok.com/" target="_"><i class="fa-brands fa-tiktok"></i></a></li>
                                <li class="float-end mr-2">doDex &copy; ${year}</li>
                            </li>
                        </div>
				  </footer>
				`
			}
		  }
		}
    }

    const dodexCard = new Map();
    dodexCard.set("Tab G:", "6")
        .set("Tab C:", "2")
        .set("Tab S:", "18")
        .set("Tab L:", "11")
        .set("Tab D:", "3");

    const tabExec = (frontCard) => {
        const front = document.querySelector(".front"+frontCard);
        const dodexElement = document.querySelector(".dodex");
        const mouseEvent = new MouseEvent("mousedown");
        front.onmousedown = dodexElement.onmousedown; // Generic dodex handler for all cards.
        front.dispatchEvent(mouseEvent);
        setTimeout(() => doDex.openDodex(), 500);
    }

    const tabEvent = (event) => {
        event.preventDefault();
        tabExec(dodexCard.get(event.target.innerHTML));
    }

    const addListenerLinks = () => {
        const links = document.querySelectorAll(".link");
        for(link of links) {
            link.addEventListener("click", () => setTimeout(() => doDex.openDodex(), 500));
        }
    }

    (async () => {
        let count = 0;
        while(document.querySelector(".back27") === null) {
            count++;
            await new Promise(r => setTimeout(r, 100));
            if(count > 50) {
                break;
            }
        }
        const back27 = document.querySelector(".back27");
        back27.classList.add("back27-handicap");
        const tabg = document.querySelector(".tab-g");
        tabg.addEventListener("click", tabEvent);
        const tabc = document.querySelector(".tab-c");
        tabc.addEventListener("click", tabEvent);
        const tabs = document.querySelector(".tab-s");
        tabs.addEventListener("click", tabEvent);
        const tabd = document.querySelector(".tab-d");
        tabd.addEventListener("click", tabEvent);
        const tabl = document.querySelector(".tab-l");
        tabl.addEventListener("click", tabEvent);
        addListenerLinks();
    })();
