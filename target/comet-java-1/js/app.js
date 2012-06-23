/**
 * bind the events to the components
 */
function loadEvents() {
	$('input[name=msg]').keyup(function(e) {
		var k = e.keyCode || e.which;
		
		if(k===13 && $(this).val() !== '') {
			sendMessage();
		}
	});
}

/**
 * post a message
 */
function sendMessage() {
	var msg = $('input[name=msg]');
	var url = 'comet';
	
	$.post(url, {
		notification: msg.val()
	}).complete(function(){
		msg.val('').focus();
	});
}

/**
 * 
 * do the long polling
 */
function listenOnlineUsers(repeat) {
	var url = "comet";
	
	$.ajax({
		url: url,
		cache: false, //cache must be false so that messages dont repeat themselves
		dataType: 'json', 
		success: function(data) {
			if(data && data.length) {
				var l = data.length;
				var i = 0;
				
				for(i; i < l; i++) {
					$('#log').prepend(data[i].msg + "<br/>");
				}
			}
			
			//when a request is complete a new one is started
			if(repeat) {
				setTimeout(function() {
					listenOnlineUsers(true);
				}, 100);
			}
			
		},
		
		//when a request is complete a new one is started
		error: function(a, b, c) {
			if(repeat) {
				setTimeout(function() {
					listenOnlineUsers(true);
				}, 100);
			}
		}
	});
}

$(document).ready(function() {
	loadEvents();
	listenOnlineUsers(true);
});