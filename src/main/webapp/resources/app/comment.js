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
        if (json.op == 'delete' && json.collection == 'comment') {
        	console.info('Deleting comment', json.data.id);
        	var comment = comments.get(json.data.id);
        	if (comment != null) {
        		// prevent sync
        		comment.id = null;
        		comment.destroy();
        	}
        }
        if (json.op == 'update' && json.collection == 'comment') {
        	console.info('Updating comment', json.data.id);
        	var comment = comments.get(json.data.id);
        	if (comment != null)
        		comment.set({'body': json.data.body, 'lastModified': json.data.lastModified});
        }
    } catch (e) {
        console.error('This doesn\'t look like a valid JSON: ', e, message.data);
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
//		comment.view = view;
//		comment.on('destroy', this.removeView);
	},
	onReset: function(comments) {
		console.log('comment list reset', this.length);
		this.each(function(comment) {
			console.log("Process comment", comment);
			var view = new CommentView({model: comment}).render();
			jQuery('#comment-stream').append(view.el);
//			comment.on('destroy', function(){ view.$el.fadeOut('slow', function(){ view.remove(); }); });
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
		this.model.on('change', this.replace, this);
		this.model.on('destroy', this.fadeAndRemove, this);
	},
	events: {
		'mouseenter'      : 'showControls',
		'mouseleave'       : 'hideControls',
		'click .edit'    : 'startEditing',
		'click .save'    : 'saveEdit',
		'click .cancel'  : 'cancelEdit',
		'click .delete'  : 'deleteEntry',
			'keypress .editor': 'editorKeypress'
	},
	render: function() {
		console.log("Rendering", this.model.get('body'), this.el);
		var template = _.template(stripCdata(jQuery('#comment-template').html()),
				{authorName: this.model.get('authorName'), body: this.model.get('body'), lastModified: this.model.get('lastModified')});
		this.$el.html(template);
		return this;
	},
	showControls: function() {
		this.$('.controls').show();
	},
	hideControls: function(e) {
		this.$('.controls').hide();
	},
	startEditing: function() {
		console.log('Edit', this.model);
		this.$('.body').hide();
		this.$('.controls').hide();
		this.$('.editor').show();
		this.$('.editor input').focus();
		this.$('.editor input').val(this.model.get('body'));
	},
	editorKeypress: function(e) {
		if (e.which == 13) {
			e.preventDefault();
			this.saveEdit();
		}
	},
	saveEdit: function() {
		this.model.set('body', this.$('.editor input').val());
		this.model.save();
	},
	cancelEdit: function() {
		this.$('.editor').hide();
		this.$('.body').show();
	},
	deleteEntry: function() {
		console.log('Delete', this.model);
		this.model.destroy();
	},
	fadeAndRemove: function() {
		var view = this;
		this.$el.fadeOut('slow', function(){ view.remove(); });
	},
	replace: function() {
		var view = this;
		this.$el.slideUp('fast', function(){ view.render(); view.$el.slideDown('slow'); });
	}
});

jQuery(document).ready(function() {

	console.info("Fetching comments...");
	comments.fetch();
	
	// Bind
	jQuery('#commentBtn').bind('click', function() {
		var commentText = jQuery('#commentBox').val();
		console.log("Comment yaaa", commentText);
		comments.create({body: commentText}, {wait: true}); // wait is needed, otherwise duplicate because Atmosphere usually got here first
	});
	jQuery('#commentBox').bind('keypress', function(e) {
		if (e.which == 13) {
			e.preventDefault();
			jQuery('#commentBtn').trigger('click');
		}
	});

	// jQuery Atmosphere
    subSocket = jQuery.atmosphere.subscribe(request);
    
});
