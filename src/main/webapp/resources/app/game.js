// jQuery Atmosphere - Setup push channel
var socket = jQuery.atmosphere;
var request = { url: baseUri + 'meteor', // TODO: add sessionId
        contentType : "application/json",
        logLevel : 'debug',
        transport : 'websocket',
        fallbackTransport: 'long-polling'};

request.onOpen = function(response) {
    console.log('Atmosphere connected using ' + response.transport);
};
request.onMessage = function (response) {
    var message = response.responseBody;
    try {
        var json = JSON.parse(message);
        console.log(json);
    } catch (e) {
        console.log('This doesn\'t look like a valid JSON: ', message.data);
        return;
    }
}
request.onError = function(response) {
    console.error('Sorry, but there\'s some problem with your '
        + 'socket or the server is down');
};

var Game = Backbone.Model.extend({
	initialize: function() {
		console.log(this.name);
	},
	defaults: {
		name: 'Default title',
		releaseDate: 2011,
	},
	url: apiUri + 'browser/game'
});
var GameCollection = Backbone.Collection.extend({
	model: Game,
	url: apiUri + 'browser/game'
});

var portal = new Game({releaseDate: 2012});
portal.url = apiUri + 'browser/game/gatotkaca';
portal.id = 'gatotkaca';

var games = new GameCollection();

jQuery(document).ready(function() {
	console.log("name is", portal.get('name'));
	portal.set({name: 'Updated'});
	console.log("new name is", portal.get('name'));
	portal.save(null, {success: function() {
		portal.set({name: 'Yet another'});
		portal.save();
	}});

	console.log("Fetching games...");
	games.fetch({success: function() {
		console.log(games.at(0).get('name'), games.at(1).get('name'))
	}});
	
	// Bind
	jQuery('#commentBtn').bind('click', function() {
		var commentText = jQuery('#commentBox').val();
		console.log("Comment yaaa", commentText);
		var comment = new Game({name: commentText});
		comment.save();
		
	});

	// jQuery Atmosphere
    subSocket = jQuery.atmosphere.subscribe(request);
    
});
