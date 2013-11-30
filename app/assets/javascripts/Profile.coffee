$(document).ready ->
  # set games table to visible, hide the progress indicator
  # and populate the table with the games
  populateProfile()

# Populates the table on the profile page
# with games or, if no games are there,
# shows the user a list of games to select as
# his first game.
populateProfile = ->
  $.get '/profile/hasGames', (bool) ->
    if bool == 'true'
      # If player has games, get all of them and put them in the table
      getGames
    else
      # Else, put the games with Abbadon in
      getGames 102

getGames = (heroId = -1) ->
  url = '/profile/games'
  if heroId != -1
    url += "/#{heroId}"
  # Queries the API for the games with the given hero (or all of them)
  jqXHR = $.get url, (json) ->
    $('#loading-indicator').remove()
    if heroId != -1
      cssHide '#first-time-text'
    cssShow '#selectable-games'

    for entry in makeTableEntries json
      $('#table-body').append entry
  jqXHR.fail (data) ->
    $('#loading-indicator').remove()
    if heroId != -1
      cssHide '#first-time-text'
    cssShow '#error-append'
    $('#error-append').append(
      "<p class=\"alert alert-danger\">" +
      "An error occurred querying the Steam API. Please try again later.</p>")

# Builds up one entry of the table in the user's profile.
makeTableEntry = (game) ->
  tableEntry = "<tr>"
  if game.details.win
    tableEntry += "<td class=\"text-success\">WIN</td>"
  else
    tableEntry += "<td class=\"text-danger\">LOSS</td>"
  dateString = (new Date game.date * 1e3).toLocaleDateString()
  tableEntry += "<td><img height=\"35px\" src=\"#{game.hero.imageUrl}\" /> #{game.hero.name}</td>"
  tableEntry += "<td>#{dateString}</td>"
  tableEntry += "</tr>"
  tableEntry

cssHide = (selector) ->
  $(selector).css 'visibility', 'hidden'

cssShow = (selector) ->
  $(selector).css 'visibility', 'visible'

makeTableEntries = (json) ->
  makeTableEntry(game) for game in json