
# 2.1.4 (2014-11-15)

* Do not stop when module dependencies cannot be fetched (failing `git archive` for instance)
* Fix ansi codes (regression introduced by 2.1.3)

# 2.1.3 (2014-11-13)

* Improve error message (do not show the whole stack trace)
* Improve logging (messages containing a `\` break, such as windows paths)

# 2.1.2 (2014-11-08)

* Show git status of modules listed by the `show` command (dirty modules are printed in yellow) 

# 2.1.1 (2014-11-03)

* Improve version parsing, now supports versions such as `puppet-apache2-2.3.10`

# 2.1.0 (2014-10-31)

* Add `show` command to display currently installed modules
* Add `only` command to focus on specific modules

# 2.0.0 (2014-10-23)

* Build first a graph of modules, validate it (no cycle and no incompatible dependencies), and finally install the modules
* Add the `graph` command
* Add the `check` and `latest` options

# 1.1.0 (2014-09-29)

* Support comments in Puppetfile

# 1.0 (2014-09-24)

* First version