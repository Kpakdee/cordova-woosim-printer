Cordova Printer-Plugin
======================

A print plugin to print a file from android/ios cordova app using third party apps. It can print both in online/offline mode.

Modified from a project from Pankaj Nirwan (not available any more - as it seems).
His project was based on the plugin https://github.com/katzer/cordova-plugin-printer

## Supported Platforms
- **Android** *(Print with woosim printer)*

## Adding the Plugin to your project
Through the [Command-line Interface](http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface):

## Important Note
To get the project to work you need to add the Woosim.jar file to your compile directory.
The file is within the lib directory

```bash

cordova plugin add https://github.com/laborc8/cordova-woosim-printer.git
cordova build


## Removing the Plugin from your project
Through the [Command-line Interface](http://cordova.apache.org/docs/en/3.0.0/guide_cli_index.md.html#The%20Command-line%20Interface):

cordova plugin rm cordova.woosim.printer
```

## Release Notes

#### Version 0.1.0 (02.09.2014)
- *Based on the Print plugin made by* ***KAtzer***
- *Based on the Print plugin made by* ***Pankaj Nirwan***

## Using the plugin
The plugin creates the object ```window.plugin.printer```


## Android




## License
-- None

