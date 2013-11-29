$(document).ready ->
  # set games table to visible, hide the progress indicator
  # and populate the table with the games
  getGames()

getGames = ->
  $.get '/profile/hasGames', (bool) ->
    # If player has games, get all of them and put them in the table
    if bool == "true"
      getGamesFor(-1)
    # Else, put the games with Abbadon in
    else
      getGamesFor(102)

getGamesFor = (heroId) ->
  url = "/profile/games"
  if heroId != -1
    url += "/#{heroId}"
  $.get url, (json) ->
    $('#loading-indicator').remove()
    if heroId != -1
      $("#first-time-text").css 'visibility', 'visible'
    $('#selectable-games').css 'visibility', 'visible'
    for entry in makeTableEntries(json)
      $('#table-body').append entry

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

makeTableEntries = (json) ->
  makeTableEntry(game) for game in json