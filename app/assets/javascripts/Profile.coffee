$(document).ready ->
  $.get '/profile/games/102', (data) ->
    # set games table to visible, hide the progress indicator
    # and populate the table with the games