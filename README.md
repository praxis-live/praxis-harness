# Praxis CORE runtime harness (experimental)

This is an experimental version of a runtime harness based around Praxis CORE v3.
It allows you to turn one or more Praxis LIVE projects into a standalone application.

Support for the runtime harness will be built into Praxis LIVE v4, offering the
ability to export standalone applications. In the meantime, the simple manual process
outlined below needs to be followed. Please test and report any issues at
https://github.com/praxis-live/support/issues Thanks!

## Getting Started

1. Download and unzip the latest release of the runtime harness from
   https://github.com/praxis-live/praxis-harness/releases The harness includes v3.5
   of Praxis CORE.
2. Prepare your project(s) - in particular, unless you want your application to be used
   from the command line, make sure that at least one of your visual root components
   (eg. video or gui) has the `exit-on-stop` property set to `true`. This ensures
   the application terminates when the window is closed.
3. Copy your project(s) into the `projects` folder inside the harness. Make sure
   to copy your project folder, not its contents.
4. Rename the harness folder to a name of your choosing.
5. Rename the following files, changing `harness` to a name of your choosing -
   this name must not contain spaces.
   - `bin/harness`
   - `bin/harness.exe`
   - `bin/harness64.exe`
   - `etc/harness.clusters`
   - `etc/harness.conf`
6. Running `bin/[appname]` (Linux / Mac) or `bin/[appname].exe` should now run
   your project!

## Advanced

There are various ways you can customise the harness to meet your needs - a UI
and automation for some of these options will be included in Praxis LIVE v4.

You can change parameters passed into the launchers (such as JVM arguments or
system properties) by editing the `etc/[appname].conf` file.

In the same `etc/[appname].conf` file you'll find reference to the `jdkhome`
property. Using this you should be able to include a JRE/JDK inside your application
folder and not have to rely on a system JVM.

Creating an installer / app bundle / package from the resulting folder is up to
you! There are various third-party plaform-specific tools available.

### Modularity

Praxis CORE is modular - you can remove modules that are not required for your
particular application from the `praxis` folder. Make sure to remove the files
from `praxis/modules`, `praxis/config/Modules` and `praxis/update_tracking`. You
can also remove additional native and Java libraries listed in the update_tracking
file for the module.

Be careful not to remove modules that are required by other modules you're using.
The `META-INF/MANIFEST.MF` file inside each module .jar lists dependencies, and a
warning dialog will open when you try to run the application if a needed module
is missing. 