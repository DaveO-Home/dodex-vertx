package dmo.fs.utils;

public final class ColorUtilConstants {
    
  private ColorUtilConstants() {
  }

    // Reset
    public static   String RESET = "\033[0m";  // Text Reset

    // Regular Colors
    public static   String BLACK = "\033[0;30m";   // BLACK
    public static   String RED = "\033[0;31m";     // RED
    public static   String GREEN = "\033[0;32m";   // GREEN
    public static   String YELLOW = "\033[0;33m";  // YELLOW
    public static   String BLUE = "\033[0;34m";    // BLUE
    public static   String PURPLE = "\033[0;35m";  // PURPLE
    public static   String CYAN = "\033[0;36m";    // CYAN
    public static   String WHITE = "\033[0;37m";   // WHITE

    // Bold
    public static   String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static   String RED_BOLD = "\033[1;31m";    // RED
    public static   String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static   String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static   String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static   String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static   String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static   String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Underline
    public static   String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
    public static   String RED_UNDERLINED = "\033[4;31m";    // RED
    public static   String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
    public static   String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
    public static   String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
    public static   String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
    public static   String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
    public static   String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

    // Background
    public static   String BLACK_BACKGROUND = "\033[40m";  // BLACK
    public static   String RED_BACKGROUND = "\033[41m";    // RED
    public static   String GREEN_BACKGROUND = "\033[42m";  // GREEN
    public static   String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
    public static   String BLUE_BACKGROUND = "\033[44m";   // BLUE
    public static   String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
    public static   String CYAN_BACKGROUND = "\033[46m";   // CYAN
    public static   String WHITE_BACKGROUND = "\033[47m";  // WHITE

    // High Intensity
    public static   String BLACK_BRIGHT = "\033[0;90m";  // BLACK
    public static   String RED_BRIGHT = "\033[0;91m";    // RED
    public static   String GREEN_BRIGHT = "\033[0;92m";  // GREEN
    public static   String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
    public static   String BLUE_BRIGHT = "\033[0;94m";   // BLUE
    public static   String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
    public static   String CYAN_BRIGHT = "\033[0;96m";   // CYAN
    public static   String WHITE_BRIGHT = "\033[0;97m";  // WHITE

    // Bold High Intensity
    public static   String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
    public static   String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
    public static   String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
    public static   String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
    public static   String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
    public static   String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
    public static   String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
    public static   String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

    // High Intensity backgrounds
    public static   String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
    public static   String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
    public static   String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
    public static   String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
    public static   String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
    public static   String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
    public static   String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
    public static   String WHITE_BACKGROUND_BRIGHT = "\033[0;107m"; // WHITE 

    public static void colorOff() {
        RESET = "";  // Text Reset
  
      // Regular Colors
        BLACK = "";   // BLACK
        RED = "";     // RED
        GREEN = "";   // GREEN
        YELLOW = "";  // YELLOW
        BLUE = "";    // BLUE
        PURPLE = "";  // PURPLE
        CYAN = "";    // CYAN
        WHITE = "";   // WHITE
  
      // Bold
        BLACK_BOLD = "";  // BLACK
        RED_BOLD = "";    // RED
        GREEN_BOLD = "";  // GREEN
        YELLOW_BOLD = ""; // YELLOW
        BLUE_BOLD = "";   // BLUE
        PURPLE_BOLD = ""; // PURPLE
        CYAN_BOLD = "";   // CYAN
        WHITE_BOLD = "";  // WHITE
  
      // Underline
        BLACK_UNDERLINED = "";  // BLACK
        RED_UNDERLINED = "";    // RED
        GREEN_UNDERLINED = "";  // GREEN
        YELLOW_UNDERLINED = ""; // YELLOW
        BLUE_UNDERLINED = "";   // BLUE
        PURPLE_UNDERLINED = ""; // PURPLE
        CYAN_UNDERLINED = "";   // CYAN
        WHITE_UNDERLINED = "";  // WHITE
  
      // Background
        BLACK_BACKGROUND = "";  // BLACK
        RED_BACKGROUND = "";    // RED
        GREEN_BACKGROUND = "";  // GREEN
        YELLOW_BACKGROUND = ""; // YELLOW
        BLUE_BACKGROUND = "";   // BLUE
        PURPLE_BACKGROUND = ""; // PURPLE
        CYAN_BACKGROUND = "";   // CYAN
        WHITE_BACKGROUND = "";  // WHITE
  
      // High Intensity
        BLACK_BRIGHT = "";  // BLACK
        RED_BRIGHT = "";    // RED
        GREEN_BRIGHT = "";  // GREEN
        YELLOW_BRIGHT = ""; // YELLOW
        BLUE_BRIGHT = "";   // BLUE
        PURPLE_BRIGHT = ""; // PURPLE
        CYAN_BRIGHT = "";   // CYAN
        WHITE_BRIGHT = "";  // WHITE
  
      // Bold High Intensity
        BLACK_BOLD_BRIGHT = ""; // BLACK
        RED_BOLD_BRIGHT = "";   // RED
        GREEN_BOLD_BRIGHT = ""; // GREEN
        YELLOW_BOLD_BRIGHT = "";// YELLOW
        BLUE_BOLD_BRIGHT = "";  // BLUE
        PURPLE_BOLD_BRIGHT = "";// PURPLE
        CYAN_BOLD_BRIGHT = "";  // CYAN
        WHITE_BOLD_BRIGHT = ""; // WHITE
  
      // High Intensity backgrounds
        BLACK_BACKGROUND_BRIGHT = "";   // BLACK
        RED_BACKGROUND_BRIGHT = "";     // RED
        GREEN_BACKGROUND_BRIGHT = "";   // GREEN
        YELLOW_BACKGROUND_BRIGHT = "";  // YELLOW
        BLUE_BACKGROUND_BRIGHT = "";    // BLUE
        PURPLE_BACKGROUND_BRIGHT = "";  // PURPLE
        CYAN_BACKGROUND_BRIGHT = "";    // CYAN
        WHITE_BACKGROUND_BRIGHT = "";   // WHITE 
      }
    
      public static void colorOn() {
        RESET = "\033[0m";  // Text Reset
  
      // Regular Colors
        BLACK = "\033[0;30m";   // BLACK
        RED = "\033[0;31m";     // RED
        GREEN = "\033[0;32m";   // GREEN
        YELLOW = "\033[0;33m";  // YELLOW
        BLUE = "\033[0;34m";    // BLUE
        PURPLE = "\033[0;35m";  // PURPLE
        CYAN = "\033[0;36m";    // CYAN
        WHITE = "\033[0;37m";   // WHITE
  
      // Bold
        BLACK_BOLD = "\033[1;30m";  // BLACK
        RED_BOLD = "\033[1;31m";    // RED
        GREEN_BOLD = "\033[1;32m";  // GREEN
        YELLOW_BOLD = "\033[1;33m"; // YELLOW
        BLUE_BOLD = "\033[1;34m";   // BLUE
        PURPLE_BOLD = "\033[1;35m"; // PURPLE
        CYAN_BOLD = "\033[1;36m";   // CYAN
        WHITE_BOLD = "\033[1;37m";  // WHITE
  
      // Underline
        BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        RED_UNDERLINED = "\033[4;31m";    // RED
        GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        WHITE_UNDERLINED = "\033[4;37m";  // WHITE
  
      // Background
        BLACK_BACKGROUND = "\033[40m";  // BLACK
        RED_BACKGROUND = "\033[41m";    // RED
        GREEN_BACKGROUND = "\033[42m";  // GREEN
        YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        BLUE_BACKGROUND = "\033[44m";   // BLUE
        PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        CYAN_BACKGROUND = "\033[46m";   // CYAN
        WHITE_BACKGROUND = "\033[47m";  // WHITE
  
      // High Intensity
        BLACK_BRIGHT = "\033[0;90m";  // BLACK
        RED_BRIGHT = "\033[0;91m";    // RED
        GREEN_BRIGHT = "\033[0;92m";  // GREEN
        YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        BLUE_BRIGHT = "\033[0;94m";   // BLUE
        PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        CYAN_BRIGHT = "\033[0;96m";   // CYAN
        WHITE_BRIGHT = "\033[0;97m";  // WHITE
  
      // Bold High Intensity
        BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE
  
      // High Intensity backgrounds
        BLACK_BACKGROUND_BRIGHT = "\033[0;100m";  // BLACK
        RED_BACKGROUND_BRIGHT = "\033[0;101m";    // RED
        GREEN_BACKGROUND_BRIGHT = "\033[0;102m";  // GREEN
        YELLOW_BACKGROUND_BRIGHT = "\033[0;103m"; // YELLOW
        BLUE_BACKGROUND_BRIGHT = "\033[0;104m";   // BLUE
        PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        CYAN_BACKGROUND_BRIGHT = "\033[0;106m";   // CYAN
        WHITE_BACKGROUND_BRIGHT = "\033[0;107m";  // WHITE 
      }
}
