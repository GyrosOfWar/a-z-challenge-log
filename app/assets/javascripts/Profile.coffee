$(document).ready ->
  # set games table to visible, hide the progress indicator
  # and populate the table with the games
  $.get '/profile/games/102', (json) ->
    $('#loading-indicator').remove()
    $('#selectable-games').css 'visibility', 'visible'
    for entry in makeTableEntries(json)
      $("#table-body").append entry


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