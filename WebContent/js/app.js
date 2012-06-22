/**
 * faz o bind dos eventos aos componentes de tela
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
 * faz um post da mensagem para o server
 */
function sendMessage() {
	var msg = $('input[name=msg]');
	var url = '/comet-test/comet';
	
	$.post(url, {
		notification: msg.val()
	}).complete(function(){
		msg.val('').focus();
	});
}

/**
 * 
 * requisição longa para os servidor
 */
function listenOnlineUsers(repeat) {
	var url = "/comet-test/comet";
	
	$.ajax({
		url: url,
		cache: false, //cache precisa ser false para nao ficar repetindo a mensagem
		dataType: 'json', //o retorno sera json sempre
		success: function(data) {
			if(data && data.length) {
				var l = data.length;
				var i = 0;
				
				for(i; i < l; i++) {
					$('#log').prepend(data[i].msg + "<br/>");
				}
			}
			
			//ao final do request, abre um novo request
			if(repeat) {
				setTimeout(function() {
					listenOnlineUsers(true);
				}, 100);
			}
			
		},
		
		//ao final do request (no caso, provavelmente um timeout), abre um novo request
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