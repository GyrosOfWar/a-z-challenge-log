$(document).ready ->
  # set games table to visible, hide the progress indicator
  # and populate the table with the games
  $.get '/profile/games/102', (json) ->
    $('#loading-indicator').css 'visibility', 'hidden'
    $('#selectable-games').css 'visibility', 'visible'
    for game in json
      tableEntry = ""
      if game.win
        tableEntry += "<tr><td class=\"text-success\">WIN</td>"
      else
        tableEntry += "<tr><td class=\"text-danger\">LOSS</td>"
      imageUrl = game.hero.imageUrl
      date = new Date game.date * 1e3
      dateString = date.toLocaleDateString()
      tableEntry += "<td><img height=\"35px\" src=\"#{imageUrl}\" /> #{game.hero.name}</td>"
      tableEntry += "<td>#{dateString}</td></tr"
      $("#table-body").append tableEntry
