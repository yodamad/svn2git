# svn2git ![Release](https://img.shields.io/github/release/yodamad/svn2git.svg?style=popout) [![Build Status](https://yodamad.visualstudio.com/svn2git/_apis/build/status/svn2git-Maven-CI?branchName=dev)](https://yodamad.visualstudio.com/svn2git/_build/latest?definitionId=1?branchName=dev)[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)![Commit](https://img.shields.io/github/last-commit/yodamad/svn2git.svg?style=flat)

This application helps you to migrate from SVN to Gitlab.

This application was generated using JHipster 5.4.2, you can find documentation and help at [jhipster](https://www.jhipster.tech/documentation-archive/v5.4.2).

## Development

To install dependences : `yarn install`

To run UI : `yarn start`

To run server : `mvn spring-boot:run`

If everything ok, you should be able to access site throught `http://localhost:9000`

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/home.png)

## Use cases
### Migration initialisation

Just click on ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/start_migration.png)

You have to check several information before starting the migration :

your gitlab account and group
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_gitlab.png)

*You can override gitlab URL & token if necessary*

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/override_gitlab.png)

your svn repository
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_svn.png)

*You can override SVN URL, user & password if necessary*

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/override_svn.png)

Then, choose between which project(s) to migration and migrate to complete repository if trunk, branches & tags are at root directory
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/choose_svn.png)

You can choose to keep history or not, only on trunk or for every branch

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/history_options.png)

If some static mappings are configured, you can enable/disable which on to apply

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/mappings.png)

Or add a custom mapping

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/custom_mapping.png)

Optionnaly, add some cleaning options based on files size or/and extensions

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/cleaning_options.png)

Or add a custom extension

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/custom_extension.png)

Check the summary
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/summary.png)

You're good to go !!

### Migration check

It is possible to check status of migrations and associated details.

Just click on ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_migration.png)

Then search migrations with your gitlab account name or svn group
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/search_migration.png)

The migrations founded are display
 
![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/migrations_list.png)

You can check details of a migration by clicking on ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/view.png)

You are routed to migration details page resuming migration and steps

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/details.png)

