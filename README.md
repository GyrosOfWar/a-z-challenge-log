AToZChallengeLog
================

A web app for the Dota 2 A-Z challenge. Uses Play! 2.2, Slick for database access (currently using H2 as in-memory
database for development, I plan on using PostegreSQL for production) and openid4java for OpenID authentication
with Steam.

How to run
================

To run, you need to create two files in the conf folder of this project: apiKey.txt (for your Steam API key) and
secretKey.conf (for the Play generated secret key that is normally included in application.conf). You can
get your API key from [http://steamcommunity.com/dev/apikey](here) and you can generate an application secret by
creating an empty Play project.