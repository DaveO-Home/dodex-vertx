/* eslint no-undef: 0 */
dodexContent = {
	cards: {
		card1: {
			tab: "A",
			front: {
				content: `<h1>Application Access</h1>
				<div class="mr-2 login-a" onclick="document.querySelector('.login').click();">
					Log in:<a href="/" class=""><i class="fa fa-sign-in"></i></a>
            	</div>`
			},
			back: {
				content: ""
			}
		},
		card2: {
			tab: "B",
			front: {
				"content": ""
			},
			back: {
				content: ""
			}
		},
		card3: {
			tab: "C",
			front: {
				content: "<h1>Best's Contact Form</h1><a ng-reflect-router-link=\"#/contact\" href=\"#/contact\"><i class=\"fa fa-fw fa-phone pr-4\"></i>Contact</a>"
			},
			back: {
				content: "<h1>Lorem Ipsum</h1><a href=\"https://www.yahoo.com\" target=\"_\">Yahoo</a>"
			}
		},
		card4: {
			tab: "D",
			front: {
				content: ""
			},
			back: {
				content: ""
			}
		},
		card5: {
			tab: "E",
			front: {
				content: ""
			},
			back: {
				content: ""
			}
		},
		card6: {
			tab: "F",
			front: {
				content: ""
			},
			back: {
				content: ""
			}
		},
		card16: {
			tab: "P",
			front: {
				content: "<h1>Test Pdf</h1><a ng-reflect-router-link=\"/pdf/test\" href=\"#/pdf/test\"><i class=\"far fa-fw fa-file-pdf pr-4\"></i>PDF View</a>"
			},
			back: {
				content: "<h1>Lorem Ipsum</h1><a href=\"https://www.yahoo.com\" target=\"_\">Yahoo16</a>"
			}
		},
		card20: {
			tab: "T",
			front: {
				content: "<h1>Test Table</h1><a ng-reflect-router-link=\"/table/tools\" href=\"#/table/tools\"><i class=\"fa fa-fw fa-table pr-4\"></i>Table View</a>"
			},
			back: {
				content: "<h1>Lorem Ipsum</h1><a href=\"https://www.yahoo.com\" target=\"_\">Yahoo20</a>"
			}
		},
		card8: {
			tab: "H",
			front: {
				content: `<h1>Description</h1>
				<a ng-reflect-router-link="/" href="#/">
					<i class="fa fa-fw fa-home pr-4"></i>Home
						<span class="sr-only">(current)</span>
				</a>`
				// <a href=\"#!\"><i class=\"fa fa-fw fa-home pr-4\"></i>Home</a>`
			},
			back: {
				content: "<h1>Lorem Ipsum</h1><a href=\"https://www.yahoo.com\" target=\"_\">Yahoo8</a>"
			}
		},
		card14: {
			tab: "N",
			front: { 
				content: '<a class="twitter-timeline" data-width="340" data-height="250" style="width:340; height:175; font-size:12;" href="https://twitter.com/TwitterDev/lists/national-parks?ref_src=twsrc%5Etfw"></a>'
			},
			back: {
				content: "<h1 style=\"font-size: 14px;\">My New Card Back</h1>"
			}
		},
		card23: {
			tab: "W",
			front: {
				content: "<h1>Angular Welcome</h1><a ng-reflect-router-link=\"/welcome\" href=\"#/welcome\"><i class=\"far fa-fw fa-hand-paper pr-4\"></i>Welcome</a>"
			},
			back: {
				content: ""
			}
		},
		card27: {
			tab: "",
			front: {
				content: ""
			},
			back: { // Bootstrap 5
				content:  `<h1 style="font-size: 14px;">
				<svg height="18" width="17" style="font-family: 'Open Sans', sans-serif;">
				<text x="3" y="18" fill="#059">O</text><text x="0" y="15" fill="#059">D</text></svg> doDex</h1>
				<footer class="dodex-footer  mt-auto py-3" style="width:95%">
						<div class="footer-col col-sm-12">
							<ul class="list-inline" class="w-100">
								<li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.facebook.com/" target="_"><i class="fab fa-fw fa-facebook"></i></a></li>
								<li class="list-inline-item"><a class="btn-social btn-outline" href="https://news.google.com/" target="_"><i class="fab fa-fw fa-google"></i></a></li>
								<li class="list-inline-item"><a class="btn-social btn-outline" href="https://twitter.com/Twitter" target="_"><i class="fab fa-fw fa-twitter"></i></a></li>
								<li class="list-inline-item"><a class="btn-social btn-outline" href="https://www.linkedin.com/" target="_"><i class="fab fa-fw fa-linkedin"></i></a></li>
								<li class="list-inline-item"><a class="btn-social btn-outline" href="https://dribbble.com/" target="_"><i class="fab fa-fw fa-dribbble"></i></a></li>
								<li class="float-end">doDex &copy; 2021</li>
							</ul>
						</div>
				</footer>`
			}
		}
	}
};
