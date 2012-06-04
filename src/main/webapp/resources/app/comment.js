//Stomp via SockJS
WebSocketStompMock = SockJS;
var client = Stomp.client('http://' + window.location.hostname + ':55674/stomp');
client.debug = function(x) { console.debug(x); };
//client.debug = pipe('#second');

//var print_first = pipe('#first', function(data) {
//  client.send('/topic/test', {}, data);
//});
client.connect('guest', 'guest', function(x) {
	id = client.subscribe("/exchange/product", function(d) {
	    var message = d.body;
	    try {
	        var json = JSON.parse(message);
	        console.log('push', json);
	        if (json['@class'] == 'org.soluvas.push.CollectionAdd' && json.collectionName == 'comment') {
//	        	var existing = comments.find(function(e){ console.debug(e.id, json.data.id); });
//	        	if (existing != null) {
//	        		console.info('Skipping existing comment', json.data.id, existing);
//	        	} else {
	    		comments.push(json.entry);
//	        	}
	        }
	        if (json['@class'] == 'org.soluvas.push.CollectionAdd' && json.collectionName == 'notification') {
	        	notificationCount.set('count', notificationCount.get('count') + 1);
	    		jQuery('#growl-container').notify('create', {text: json.entry.message});
	        }
	        if (json['@class'] == 'org.soluvas.push.CollectionDelete' && json.collectionName == 'comment') {
	        	console.info('Deleting comment', json.entryId);
	        	var comment = comments.get(json.entryId);
	        	if (comment != null) {
	        		// prevent sync
	        		comment.id = null;
	        		comment.destroy();
	        	}
	        }
	        if (json['@class'] == 'org.soluvas.push.CollectionUpdate' && json.collectionName == 'comment') {
	        	console.info('Updating comment', json.entryId);
	        	var comment = comments.get(json.entryId);
	        	if (comment != null)
	        		comment.set({'body': json.entry.body, 'lastModified': json.entry.lastModified});
	        }
	    } catch (e) {
	        console.error('This doesn\'t look like a valid JSON: ', e, message);
	    }
	});
});

// Models
var commentTemplate = '<strong><%=authorName%></strong>\
    <div class="body"> \
        <%=body%> &#183; <span style="color: #888; font-size: 80%;"><%=lastModified%></span></div> \
    <div class="editor" style="display: none"> \
	    <input type="text"/> \
	    <button class="btn save"><i class="icon-ok"></i></button> \
	</div> \
    <div class="controls" style="position: absolute; top: 0; right: 0; display: none;"> \
        <button class="btn edit" title="Edit"><i class="icon-edit"></i></button> \
        <button class="btn delete" title="Delete"><i class="icon-remove"></i></button> \
    </div>';
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
		jQuery('#commentStream').html('<p>Eh lucu</p>');
		this.each(function(comment) {
			console.log("Process comment", comment);
			var view = new CommentView({model: comment}).render();
			jQuery('#comment-stream').append(view.$el);
		});
	}
});

var NotificationCount = Backbone.Model.extend({
	defaults: {count: 0},
	initialize: function(args) {
		_.bindAll(this, 'render');
		this.on('change', this.render);
	},
	render: function() {
		console.info('notificationCount render', this.get('count'));
		var template = '';
		if (this.get('count') > 0)
			template = _.template('<span style="background: red; color: white"> <%=count%> </span>', {count: this.get('count')});
		jQuery('#notification-count').html(template);
	}
});
var notificationCount = new NotificationCount;

var CommentView = Backbone.View.extend({
	tagName: 'li',
	className: 'comment',
	initialize: function(args) {
		_.bindAll(this, 'render', 'fadeAndRemove', 'remove');
		this.model.on('change', this.replaceEntry, this);
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
		var template = _.template(commentTemplate,
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
		this.$el.fadeOut('slow', this.remove);
	},
	replaceEntry: function() {
		var view = this;
		this.$el.slideUp('fast', function(){ view.render(); view.$el.slideDown('slow'); });
	}
});

var freshComment = new Comment;

var comments = new CommentList;

function stripCdata(str) {
	return (str+'').replace(/\<\!\[CDATA\[|\]\]\>/g, '');
}
function convertTemplate(str) {
	return str.replace(/\{\{/g, '<%=').replace(/\}\}/g, '%>');
}

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
	jQuery('#test-push').bind('click', function() {
		jQuery('#growl-container').notify('create', {title: 'Wah keren', text: 'Maknyus gan'});
	});
	jQuery('#notification-count').bind('click', function() {
		notificationCount.set('count', 0);
	});

});
