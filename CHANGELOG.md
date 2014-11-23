
# 2.2.1 _(2014-11-23)_

* Use [backuity/ansi-interpolator](https://github.com/backuity/ansi-interpolator), cutting down one dependency (shrinking the one-jar by 400KB)

# 2.2.0 _(2014-11-18)_

* Add the `--dont-recurse` option

# 2.1.4 _(2014-11-15)_

* Do not stop when module dependencies cannot be fetched (failing `git archive` for instance)
* Fix ansi codes (regression introduced by 2.1.3)

# 2.1.3 _(2014-11-13)_

* Improve error message (do not show the whole stack trace)
* Improve logging (messages containing a `\` break, such as windows paths)

# 2.1.2 _(2014-11-08)_

* Show git status of modules listed by the `show` command (dirty modules are printed in yellow) 

# 2.1.1 _(2014-11-03)_

* Improve version parsing, now supports versions such as `puppet-apache2-2.3.10`

# 2.1.0 _(2014-10-31)_

* Add `show` command to display currently installed modules
* Add `only` command to focus on specific modules

# 2.0.0 _(2014-10-23)_

* Build first a graph of modules, validate it (no cycle and no incompatible dependencies), and finally install the modules
* Add the `graph` command
* Add the `check` and `latest` options

# 1.1.0 _(2014-09-29)_

* Support comments in Puppetfile

# 1.0 _(2014-09-24)_

* First version
