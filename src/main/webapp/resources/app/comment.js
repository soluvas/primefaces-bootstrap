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
        console.log('push', json);
        if (json.op == 'add' && json.collection == 'comment') {
//        	var existing = comments.find(function(e){ console.debug(e.id, json.data.id); });
//        	if (existing != null) {
//        		console.info('Skipping existing comment', json.data.id, existing);
//        	} else {
        		comments.push(json.data);
//        	}
        }
    } catch (e) {
        console.log('This doesn\'t look like a valid JSON: ', message.data);
        return;
    }
}
request.onError = function(response) {
    console.error('Sorry, but there\'s some problem with your '
        + 'socket or the server is down');
};

var Comment = Backbone.Model.extend({
	defaults: {
		authorName: 'Hendy Irawan',
		body: 'Untitled comment',
		created: new Date(),
		lastModified: new Date()
	}
});
var CommentList = Backbone.Collection.extend({
	model: Comment,
	url: apiUri + 'comment',
	initialize: function() {
		this.on('reset', this.onReset);
		this.on('add', this.onAdd);
		this.on('change', this.onChange);
	},
	onChange: function(arg) {
		console.log('comment list changed', arg);
	},
	onAdd: function(comment) {
		console.log('comment list added', comment);
		var view = new CommentView({model: comment}).render();
		view.$el.hide();
		jQuery('#comment-stream').append(view.el);
		view.$el.fadeIn('slow');
	},
	onReset: function(comments) {
		console.log('comment list reset', this.length);
		this.each(function(e) {
			console.log("Process comment", e);
			var view = new CommentView({model: e}).render();
			jQuery('#comment-stream').append(view.el);
		});
	}
});

var freshComment = new Comment;

var comments = new CommentList;

function stripCdata(str) {
	return str.substring(9, str.length - 4);
}

var CommentView = Backbone.View.extend({
	tagName: 'li',
	className: 'comment',
	initialize: function(args) {
		//_.bindAll(this, 'changeName');
		//this.model.bind('change:name', this.changeName);
		//this.render();
	},
	events: {
		'click .edit'  : 'edit',
		'click .delete': 'deleteEntry'
	},
	render: function() {
		console.log("Rendering", this.model.get('body'), this.el);
		var template = _.template(stripCdata(jQuery('#comment-template').html()),
				{authorName: this.model.get('authorName'), body: this.model.get('body')});
		this.$el.html(template);
		return this;
	},
//	changeName: function() {
//		console.log("Change name", this);
//	},
	edit: function() {
		console.log('Edit', this.model);
		this.model.set('body', 'baru yaaaa');
		this.model.save();
	},
	deleteEntry: function() {
		console.log('Delete', this.model);
		this.model.destroy();
	}
});

jQuery(document).ready(function() {
//	console.log("name is", portal.get('name'));
//	portal.set({name: 'Updated'});
//	console.log("new name is", portal.get('name'));
//	portal.save(null, {success: function() {
//		portal.set({name: 'Yet another'});
//		portal.save();
//	}});

	console.log("Fetching comments...");
	comments.fetch();
	
	// Bind
	jQuery('#commentBtn').bind('click', function() {
		var commentText = jQuery('#commentBox').val();
		console.log("Comment yaaa", commentText);
		comments.create({body: commentText}, {wait: true}); // wait is needed, otherwise duplicate because Atmosphere usually got here first
	});

	// jQuery Atmosphere
    subSocket = jQuery.atmosphere.subscribe(request);
    
});
