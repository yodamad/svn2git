# svn2git
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

your gitlab account : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_gitlab.png)

your gitlab group : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_group.png)

your svn repository : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_svn.png)

Then, chose which project(s) to migration : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/choose_svn.png)

Optionnaly, add some cleaning options based on files size or/and extensions : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/cleaning_options.png)

Check the summary : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/summary.png)

You're good to go !!

### Migration check

It is possible to check status of migrations and associated details.

Just click on ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/check_migration.png)

Then search migrations with your gitlab account name or svn group : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/search_migration.png)

The migrations founded are display : ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/migrations_list.png)

You can check details of a migration by clicking on ![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/view.png)

You are routed to migration details page resuming migration and steps

![alt text](https://raw.githubusercontent.com/yodamad/svn2git/master/github/details.png)

