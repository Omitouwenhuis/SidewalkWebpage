/** @namespace */
var sg = sg || {};

/**
 * Main module for Gallery.
 * @param params Object passed from gallery.scala.html containing initial values pulled from the database on page
 *              load.
 * @returns {Main}
 * @constructor
 */
function Main (params) {
    let self = this;

    sg.scrollStatus = {
        stickySidebar: true,
        stickyModal: true
    };

    let headerSidebarOffset = undefined;

    function _initUI() {
        sg.ui = {};

        // Initializes filter components in side bar.
        sg.ui.cardFilter = {};
        sg.ui.cardFilter.wrapper = $(".sidebar");
        sg.ui.cardFilter.holder = $("#card-filter");
        sg.ui.cardFilter.tags = $("#tags");
        sg.ui.cardFilter.severity = $("#severity");

        // Initializes label select component in side bar.
        sg.ui.ribbonMenu = {};
        sg.ui.ribbonMenu.holder = $("#ribbon-menu-holder");
        sg.ui.ribbonMenu.select = $('#label-select');

        // TODO: potentially remove if we decide sorting is not desired for later versions.
        sg.ui.cardSortMenu = {};
        sg.ui.cardSortMenu.holder = $("#card-sort-menu-holder");
        sg.ui.cardSortMenu.sort = $('#card-sort-select');

        // Initialize card container component.
        sg.ui.cardContainer = {};
        sg.ui.cardContainer.holder = $("#image-card-container");
        sg.ui.cardContainer.prevPage = $("#prev-page");
        sg.ui.cardContainer.pageNumber = $("#page-number")
        sg.ui.cardContainer.nextPage = $("#next-page");

        // Keep track of the next/prev arrow container.
        sg.ui.pageControl = $(".page-control");

        // Keep track of navbar.
        sg.ui.navbar = $("#header")

        $('.gallery-modal').hide();

        // Calculate offset between bottom of navbar and sidebar.
        headerSidebarOffset = sg.ui.cardFilter.wrapper.offset().top - (sg.ui.navbar.offset().top + sg.ui.navbar.outerHeight());
    }

    function _init() {
        sg.rootDirectory = ('rootDirectory' in params) ? params.rootDirectory : '/';

        // Initialize functional components of UI elements.
        sg.ribbonMenu = new RibbonMenu(sg.ui.ribbonMenu);
        // sg.cardSortMenu = new CardSortMenu(sg.ui.cardSortMenu);
        sg.tagContainer = new CardFilter(sg.ui.cardFilter, sg.ribbonMenu);
        sg.cardContainer = new CardContainer(sg.ui.cardContainer);

        // Initialize data collection.
        sg.form = new Form(params.dataStoreUrl, params.beaconDataStoreUrl)
        sg.tracker = new Tracker();

        sg.util = {};

        // // Set initial sidebar stickiness
        // let initSidebarBottomOffset = sg.ui.cardFilter.wrapper.offset().top +
        //                               sg.ui.cardFilter.wrapper.outerHeight(true);
        // let initCardContainerBottomOffset = sg.ui.cardContainer.holder.offset().top +
        //                                     sg.ui.cardContainer.holder.outerHeight(true) - 10;
        // status.stickySidebar = initCardContainerBottomOffset < initSidebarBottomOffset;

        $(window).scroll(function () {
            // Make sure the page isn't loading.
            if (!$("#page-loading").is(":visible")) {
                let sidebarBottomOffset = sg.ui.cardFilter.wrapper.offset().top +
                                          sg.ui.cardFilter.wrapper.outerHeight(true);
                let cardContainerBottomOffset = sg.ui.cardContainer.holder.offset().top +
                                                sg.ui.cardContainer.holder.outerHeight(true) - 10;
                let pageControlTopOffset = sg.ui.pageControl.offset().top;
                let visibleWindowBottomOffset = $(window).scrollTop() + $(window).height();

                // Handle sidebar stickiness.
                if (sg.scrollStatus.stickySidebar) {
                    console.log("are we in here");
                    if (pageControlTopOffset < sidebarBottomOffset) {
                        console.log("triggered");
                        sidebarHeightBeforeRelative = sg.ui.cardFilter.wrapper.outerHeight(true);

                        // Adjust sidebar positioning.
                        sg.ui.cardFilter.wrapper.css('position', 'relative');

                        // The sg.ui.navbar.outerHeight(false) is necessary in order for the placement of the sidebar
                        // to be correct once it goes to being relatively positioned.
                        // TODO: investigate more closely as to why.
                        let navbarHeight = sg.ui.navbar.outerHeight(false);
                        let newTop = pageControlTopOffset - sidebarHeightBeforeRelative - navbarHeight;
                        console.log(newTop);
                        sg.ui.cardFilter.wrapper.css('top', newTop);

                        // Adjust card container margin.
                        sg.ui.cardContainer.holder.css('margin-left', '0px');
                        sg.scrollStatus.stickySidebar = false;
                    }
                } else {
                    if (!sg.scrollStatus.stickySidebar) {
                        currHeaderSidebarOffset = sg.ui.cardFilter.wrapper.offset().top -
                                                 (sg.ui.navbar.offset().top + sg.ui.navbar.outerHeight(false));
                        if (currHeaderSidebarOffset > headerSidebarOffset) {
                            // Adjust sidebar positioning.
                            sg.ui.cardFilter.wrapper.css('position', 'fixed');
                            sg.ui.cardFilter.wrapper.css('top', '');
        
                            // Adjust card container margin.
                            sg.ui.cardContainer.holder.css('margin-left', '235px'); // constant
                            sg.scrollStatus.stickySidebar = true;
                        }
                    }
                }

                // Handle modal stickiness.
                if (cardContainerBottomOffset < visibleWindowBottomOffset) {
                    if (sg.scrollStatus.stickyModal) {
                        // Prevent modal from going too low (i.e., when a user scrolls down fast).
                        $('.gallery-modal').css('top', cardContainerBottomOffset - $(window).height());
                        sg.scrollStatus.stickyModal = false;
                    }
                } else {
                    if (!sg.scrollStatus.stickyModal) sg.scrollStatus.stickyModal = true;
                    
                    // Emulate the modal being "fixed".
                    $('.gallery-modal').css('top', $(window).scrollTop());
                }
            }
        }); 
    }

    // Gets all the text on the gallery page for the correct language.
    i18next.use(i18nextXHRBackend);
    i18next.init({
        backend: { loadPath: '/assets/locales/{{lng}}/{{ns}}.json' },
        fallbackLng: 'en',
        ns: ['common', 'gallery'],
        defaultNS: 'common',
        lng: params.language,
        debug: false
    }, function(err, t) {
        if (err) return console.log('something went wrong loading', err);

        _initUI();
        _init();
    });

    return self;
}
