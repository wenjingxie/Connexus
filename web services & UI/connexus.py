import os
import urllib
import json
import datetime

from google.appengine.api import users
from google.appengine.api import images
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import urlfetch
from google.appengine.ext import blobstore
from google.appengine.ext.webapp import blobstore_handlers

import webapp2


frequency = 0 
count = 0
autoData = []
autoCompleteStatus = False

# define datastructure to store streams
class Stream(ndb.Model):
	name = ndb.StringProperty()
	owner = ndb.StringProperty()
	subscriber = ndb.StringProperty(repeated = True)
	tags = ndb.StringProperty(repeated = True)
	photos = ndb.BlobKeyProperty(repeated = True)
	creation_time = ndb.DateTimeProperty(auto_now_add=True)  	
	cover = ndb.StringProperty()
	viewRecord = ndb.DateTimeProperty(repeated = True)
	viewCount = ndb.IntegerProperty()
	geo = ndb.JsonProperty(repeated = True)

class MainPage(webapp2.RequestHandler):

    def get(self):
        # Checks for active Google account session
        user = users.get_current_user()
        if user:
        	# send request to service to get streams and subscirbe strems for this user
        	user = user.nickname()
        	params = {'owner': user}
        	url = 'http://aptconnexus11.appspot.com/manageAPI'
        	result = jsonfyFetch(params, url)
        	self.response.write("""<!doctype html><html>
        		<head><style>table, th, td {border: 1px solid black;
        		border-collapse: collapse;}th, td {padding: 5px;}</style></head>
        		<body>
        		<h1> Connex.us </h1>
        		<a href = "/"> Manage </a>
        		<a href = "/create"> | Create </a>
        		<a href = "/viewAll"> | View </a>
        		<a href = "/search"> | Search </a>
        		<a href = "/trending"> | Trending </a>
        		<a href = "/social"> | Social </a>
        		</p>""")
        	# streams I own
        	self.response.write("""<h2>Streams I own</h2>
        		<form action = "/deleteOwnStream" method = "post">
        		<table><tr><th>Name</th><th>Last New Picture</th>
        		<th>Number of Pictures</th><th>Delete</th></tr>""")
        	ownStreams = result['ownStream']
        	for stream in ownStreams:
        		streamKey = stream.keys()[0]
        		name = stream[streamKey][0]
        		date = stream[streamKey][1]
        		num = stream[streamKey][2]
        		imgKey = stream[streamKey][3]
        		self.response.write("""<tr><td><a href = "/viewSingle/%s/%s/1"</a>%s</td>
        			<td>%s</td><td>%s</td><td><input type="checkbox" name = "%s"></td>
        			"""%(streamKey, imgKey, name, date, num, streamKey))
        	self.response.write("""<tr><td><input type = "submit" value = "Delete Checked"></td></tr></table></form>""")
        	
        	# streams I subscribe to
        	self.response.write("""<h2>Streams I subscribe to</h2>
         		<form action = "/unsubscribe" method = "post">
         		<table><tr><th>Name</th><th>Last New Picture</th>
         		<th>Number of Pictures</th><th>Views</th><th>Unsubscribe</th></tr>""")
        	subStreams = result['subStream']
        	for stream in subStreams:
        		streamKey = stream.keys()[0]
        		name = stream[streamKey][0]
        		date = stream[streamKey][1]
        		num = stream[streamKey][2]
        		viewCount = stream[streamKey][3]
        		imgKey = stream[streamKey][4]
        		self.response.write("""<tr><td><a href = "/viewSingle/%s/%s/1"</a>%s</td>
         			<td>%s</td><td>%s</td><td>%s</td><td><input type="checkbox" name = "%s"></td>
         			"""%(streamKey, imgKey, name, date, num, viewCount,streamKey))
        	self.response.write("""<tr><td><input type = "submit" value = "Unsubscribe Checked Streams"></td></tr></table></form>""")
        	self.response.write("</body></html>")
        else:
        	self.redirect(users.create_login_url(self.request.uri))

class ManageAPI(webapp2.RequestHandler):

	def post(self):
		updateViewCount()
		owner = json.loads(self.request.body)['owner']
		streamsOwn = Stream.query(Stream.owner == owner).order(Stream.creation_time)
		streamsSub = Stream.query(Stream.subscriber == owner).order(-Stream.viewCount)
		result = {}
		ownStream = []
		subStream = []
		for stream in streamsOwn:
			if len(stream.photos) >= 1:
				lastNewPic = blobstore.BlobInfo.get(stream.photos[len(stream.photos) - 1])
				lastDate = lastNewPic.creation.date()
				ownStream.append({stream.key.urlsafe(): [stream.name, str(lastDate), len(stream.photos),str(stream.photos[len(stream.photos) - 1])]})
			else:
				ownStream.append({stream.key.urlsafe(): [stream.name, '/', 0, 'no']})
		result['ownStream'] = ownStream
		for stream in streamsSub:
			if len(stream.photos) >= 1:
				lastNewPic = blobstore.BlobInfo.get(stream.photos[len(stream.photos) - 1])
				lastDate = lastNewPic.creation.date()
				subStream.append({stream.key.urlsafe(): [stream.name, str(lastDate), len(stream.photos), stream.viewCount, str(stream.photos[len(stream.photos) - 1])]})
			else:
				subStream.append({stream.key.urlsafe(): [stream.name, '/', 0, stream.viewCount,'no']})
		result['subStream'] = subStream
		self.response.write(json.dumps(result))

class DeleteOwnStream(webapp2.RequestHandler):
	def post(self):
		owner = users.get_current_user().nickname()
		streamsOwn = Stream.query(Stream.owner == owner).order(Stream.name)
		params = {}
		params['deleteOwn'] = []
		for stream in streamsOwn:
			streamkey = stream.key.urlsafe()
			delete = self.request.get(streamkey)
			if delete:
				params['deleteOwn'].append(streamkey)
		url = 'http://aptconnexus11.appspot.com/deleteAPI'
		result = jsonfyFetch(params, url)
		self.redirect('/')

class DeleteAPI(webapp2.RequestHandler):
	def post(self):
		params = json.loads(self.request.body)
		if 'deleteOwn' in params:
			for streamKeyUrl in params['deleteOwn']:
				streamKey = ndb.Key(urlsafe=streamKeyUrl)
				stream = streamKey.get()
				streamKey.delete()
				stream.put()
		if 'unsubscribe' in params:
			owner = params['user']
			for streamKeyUrl in params['unsubscribe']:
				streamKey = ndb.Key(urlsafe=streamKeyUrl)
				stream = streamKey.get()
				stream.subscriber.remove(owner)
				stream.put()
		self.response.write(json.dumps({'url': '/'}))

class Unsubscribe(webapp2.RequestHandler):
	def post(self):
		owner = users.get_current_user().nickname()
		streamsSub = Stream.query(Stream.subscriber == owner).order(Stream.name)
		params = {}
		params['unsubscribe'] = []
		params['user'] = owner
		for stream in streamsSub:
			streamkey = stream.key.urlsafe()
			unSub = self.request.get(streamkey)
			if unSub:
				params['unsubscribe'].append(streamkey)
		url = 'http://aptconnexus11.appspot.com/deleteAPI'
		result = jsonfyFetch(params, url)
		self.redirect('/')

class Create(webapp2.RequestHandler):

	def get(self):
		user = users.get_current_user()
		if user:
			self.response.write("""<!doctype html><html><body>
        		<h1> Connex.us </h1>
        		<p>
        		<a href = "/"> Manage </a>
        		<a href = "/create"> | Create </a>
        		<a href = "/viewAll"> | View </a>
        		<a href = "/search"> | Search </a>
        		<a href = "/trending"> | Trending </a>
        		<a href = "/social"> | Social </a>
        		</p>
        		<form name="input" action="/createSubmit" method="post">
                Name your stream: <input type="text" name="streamName"><br>
                Tag your stream: <textarea name="streamTags" rows="3" cols="20"></textarea><br>
                URL to cover image(Can be empty): <input type="text" name="streamCover"><br><br><br>
                Add subscribers <br>
                Emails: <textarea name="email" rows = "3" cols= "20"></textarea><br>
                Optional messages for invite: <textarea name="message" rows = "3" cols= "20"></textarea><br><br><br>
                <input type="submit" value="Create Stream">
                </form>
        		</body></html>""")
		else:
			self.redirect(users.create_login_url(self.request.uri))

# handle the submit action, Jsonfy the data, send to server
class CreateSubmit(webapp2.RequestHandler):

	def post(self):
		owner = users.get_current_user().nickname()
		name = self.request.get('streamName')
		tags = self.request.get('streamTags').split(',')
		cover = self.request.get('streamCover')
		email = self.request.get('email')
		if len(email) != 0:
			email = email.split(',')
		else:
			email = []
		message = self.request.get('message')
		params = {'owner': owner, 'name': name, 'tags': tags, 'cover': cover, 'email': email, 'message': message}
		url = 'http://aptconnexus11.appspot.com/createSubmitAPI'
		result = jsonfyFetch(params,url)
		self.redirect(result['url'])

class CreateSubmitAPI(webapp2.RequestHandler):

	def post(self):
		params = json.loads(self.request.body)
		# create ndb model
		sameName = Stream.query(Stream.name == params['name'])
		num = 0
		for item in sameName:
			num += 1
		if num == 0:
			stream = Stream()
			stream.owner = params['owner']
			stream.name = params['name']
			stream.tags = params['tags']
			stream.cover = params['cover']
			stream.put()

			# send emails to invite subscribes
			emails = params['email']
			message = params['message']
			if len(emails) != 0:
				for email in emails:
					mail.send_mail(sender="<vinckyxie@gmail.com>",
                      to="<%s>"%email,
                      subject="Connexus",
                      body="""Here is Connexus! %s wants to invite you to subscribe his stream. %s
                      """%(params['owner'], params['message']))
			self.response.write(json.dumps({'url': '/'}))
		else:
			self.response.write(json.dumps({'url': '/error'}))





class ViewAll(webapp2.RequestHandler):

	def get(self):
		user = users.get_current_user()
		if user:
	 		params = {'task': 'viewAll'}
	 		url = 'http://aptconnexus11.appspot.com/viewAllAPI'
	 		result = jsonfyFetch(params, url)
			self.response.write("""<!doctype html><html><body>
        		<h1> Connex.us </h1>
        		<p>
        		<a href = "/"> Manage </a>
        		<a href = "/create"> | Create </a>
        		<a href = "/viewAll"> | View </a>
        		<a href = "/search"> | Search </a>
        		<a href = "/trending"> | Trending </a>
        		<a href = "/social"> | Social </a>
        		</p>
        		<h2> View All Streams </h2>""")
			i = 0
			for stream in result:
				key = stream.keys()[0]
				if i % 4 == 0:
					self.response.write("<p>")
				self.response.write("""<a href = "/viewSingle/%s/%s/1"> 
                   <img src = "%s" style="width:200px;height:150px" alt = "%s"></a>"""%(key, stream[key][2], stream[key][1], stream[key][0]))
				if i % 4 == 3:
					self.response.write("</p>")
				i += 1
			self.response.write("</body></html>")
		else:
			self.redirect(users.create_login_url(self.request.uri))





class ViewAllAPI(webapp2.RequestHandler):

	def post(self):
		streams = Stream.query().order(Stream.creation_time)
		result = []
		for stream in streams:
			if len(stream.photos) >= 1:
				result.append({stream.key.urlsafe(): [stream.name, stream.cover, str(stream.photos[len(stream.photos) - 1])]})
			else:
				result.append({stream.key.urlsafe(): [stream.name, stream.cover, 'no']})
		self.response.write(json.dumps(result))


class UploadHandler(blobstore_handlers.BlobstoreUploadHandler):

	def post(self, resource):
		streamKeyUrl = str(urllib.unquote(resource))
		streamKey = ndb.Key(urlsafe=streamKeyUrl)
		stream = streamKey.get()
		upload_files = self.get_uploads('file')  # 'file' is file upload field in the form
		blob_info = upload_files[0]
		imageKey = blob_info.key()
		stream.photos.append(imageKey)
		stream.put()
		self.redirect('/viewSingle/%s/%s/0'%(streamKeyUrl, imageKey))



class ImageUrlAPI(blobstore_handlers.BlobstoreDownloadHandler):

 	def post(self):
 		blobKeyStr = json.loads(self.request.body)['blobKey']
 		blobKey = blobstore.BlobKey(blobKeyStr)
		url = images.get_serving_url(blobKey, size=None, crop=False, secure_url=None)
		self.response.write(json.dumps({'url': url}))

class Subscribe(webapp2.RequestHandler):

	def get(self, streamKey, imageKey):
		streamKeyUrl = str(urllib.unquote(streamKey))
		imageKeyStr = str(urllib.unquote(imageKey))
		user = users.get_current_user().nickname()
		params = {'user': user, 'streamKeyUrl': streamKeyUrl}
		url = 'http://aptconnexus11.appspot.com/subscribeAPI'
		result = jsonfyFetch(params, url)
		self.redirect('/viewSingle/%s/%s/0'%(streamKeyUrl, imageKeyStr))

class SubscribeAPI(webapp2.RequestHandler):

	def post(self):
		params = json.loads(self.request.body)
		user = params['user']
		streamKeyUrl = params['streamKeyUrl']
		streamkey = ndb.Key(urlsafe=streamKeyUrl)
		stream = streamkey.get()
		stream.subscriber.append(user)
		stream.put()
		self.response.write(json.dumps({'status': 'done'}))


class autoDataAPI(webapp2.RequestHandler):
	def get(self):
		global autoData
		result = []
		streams = Stream.query().order(Stream.creation_time)

		for stream in streams:
			var = {"label":stream.name, "value":stream.tags}
			result.append(var)
		autoData = result
		self.response.write(json.dumps(result))
		

class autocompleteAPI(webapp2.RequestHandler):
	def get(self, response):
		global autoData
		self.response.write(json.dumps(autoData))


class Search(webapp2.RequestHandler):
	
	def get(self):
		global autoData,autoCompleteStatus
		if not autoCompleteStatus:
			autoCompleteStatus = True
			streams = Stream.query().order(Stream.creation_time)
			for stream in streams:
				var = {"label":stream.name, "value":stream.tags}
				autoData.append(var)
		self.response.write("""<!doctype html><html>
            <link rel='stylesheet' type='text/css' href='http://aptconnexus11.appspot.com/auto/css/jquery-ui.css'/>
            <script type="text/javascript" src="http://aptconnexus11.appspot.com/auto/js/jquery-2.1.1.js"></script>
            <script type="text/javascript" src="http://aptconnexus11.appspot.com/auto/js/jquery-ui.js"></script>
            <script>
                $(function() {
                    var availableTutorials = ["Andy", "Andrew", "Bob", "Cane", "Name"];
					var testdata = [{label: "male", value: "jack"}, {label: "female", value: "jane"}, {label: "male", value: "john"}];
					var cache = {};
					var remotedata = [];
					var remotelist = [];
					$.getJSON("http://aptconnexus11.appspot.com/autocompleteAPI", function(data){
						remotedata = data;
						for(var i=0; i<remotedata.length; i++){
							var obj = remotedata[i];
							console.log(obj);
							var termExist = remotelist.indexOf(obj.label);
							if(termExist = -1){
								remotelist.push(obj.label);
								console.log("pushed " + obj.label);
							}
							for(var j=0; j<obj.value.length; j++){
								termExist = remotelist.indexOf(obj.value[j]);
								if(termExist = -1){
									remotelist.push(obj.value[j]);
								}
							}
							console.log(remotelist);
						}
					});
                    $( "#auto_input" ).autocomplete({
						max: 1,
						delay: 200,
                        minLength: 2,
                        source: remotelist
                    });
                });
            </script>
            <body>
                <h1> Connex.us </h1>
                <p>
                <a href = "/"> Manage </a>
                <a href = "/create"> | Create </a>
                <a href = "/viewAll"> | View </a>
                <a href = "/search"> | Search </a>
                <a href = "/trending"> | Trending </a>
                <a href = "/social"> | Social </a>
                </p>""")
		# search form, search button
		self.response.write("""<form name="input" action="/searchSubmit" method="get">
                Keyword to search: <input id="auto_input" type="text" name="keyword"><br>
                <input type="submit" value="Search">
                </form>
			</body></html>""")

class SearchSubmit(webapp2.RequestHandler):

	def get(self):
		keyword = self.request.get('keyword')
		params = {'keyword': keyword}
		url = 'http://aptconnexus11.appspot.com/searchAPI'
		result = jsonfyFetch(params, url)


		self.response.write("""<!doctype html><html><body>
         		<h1> Connex.us </h1>
         		<p>
         		<a href = "/"> Manage </a>
         		<a href = "/create"> | Create </a>
         		<a href = "/viewAll"> | View </a>
         		<a href = "/search"> | Search </a>
         		<a href = "/trending"> | Trending </a>
         		<a href = "/social"> | Social </a>
         		</p>""")
		self.response.write("""<p><form name="input" action="/searchSubmit" method="get">
                Keyword to search: <input type="text" name="keyword" value = "%s"> <br>
                <input type="submit" value="Search">
                </form></p>"""%keyword)
		self.response.write("""<p> %s results for <strong>%s</strong> <br>
								click on an image to view stream</p> """%(len(result), keyword))
		for key in result:
			self.response.write("""<p><a href = "/viewSingle/%s/%s/1"> 
                   <img src = "%s" style="width:304px;height:228px" alt = "%s"></a></p>"""%(key, result[key][2], result[key][1], result[key][0]))
		self.response.write("</body></html>")



class SearchAPI(webapp2.RequestHandler):

	def post(self):
		params = json.loads(self.request.body)
		keyword = params['keyword']
		streams = Stream.query(ndb.OR(Stream.name == keyword, Stream.tags == keyword))
		result = {}
		for stream in streams:
			if len(stream.photos) >= 1:
				result[stream.key.urlsafe()] = [stream.name, stream.cover, str(stream.photos[len(stream.photos) - 1])]
			else:
				result[stream.key.urlsafe()] = [stream.name, stream.cover, 'no']
		self.response.write(json.dumps(result))

class Trending(webapp2.RequestHandler):
	def get(self):
		global frequency
		params = {}
		url = 'http://aptconnexus11.appspot.com/trendingAPI'
		result = jsonfyFetch(params, url)

		# main menu
		self.response.write("""<!doctype html><html><body>
         		<h1> Connex.us </h1>
         		<p>
         		<a href = "/"> Manage </a>
         		<a href = "/create"> | Create </a>
         		<a href = "/viewAll"> | View </a>
         		<a href = "/search"> | Search </a>
         		<a href = "/trending"> | Trending </a>
         		<a href = "/social"> | Social </a>
         		</p><h2>Top 3 Trending Streams</h2><table><tr>""")

		# top 3 streams
		for stream in result:
			streamKeyUrl = stream.keys()[0]
			streamName = stream[streamKeyUrl][0]
			self.response.write("<td>%s</td>"%streamName)
		self.response.write("</tr><tr>")
		for stream in result:
			streamKeyUrl= stream.keys()[0]
			streamName = stream[streamKeyUrl][0]
			streamCover = stream[streamKeyUrl][1]
			imgKey = stream[streamKeyUrl][2]
			self.response.write("""<td><a href = "/viewSingle/%s/%s/1"> 
                   <img src = "%s" style="width:200px;height:150px" alt = "%s"></a></td>"""%(streamKeyUrl, imgKey, streamCover, streamName))
		self.response.write("</tr><tr>")
		for stream in result:
			streamKeyUrl = stream.keys()[0]
			streamViewCount = stream[streamKeyUrl][3]
			self.response.write("<td>%s views in past hour</td>"%streamViewCount)

		# update email rate
		noReport = ""
		fiveMin = ""
		oneHour = ""
		oneDay = ""
		if frequency == 0:
			noReport = "checked"
		elif frequency == 1:
			fiveMin = "checked"
		elif frequency == 12:
			oneHour = "checked"
		else:
			oneDay = "checked"
		self.response.write("""</tr></table><p>
				<form name="UpdateRate" action="/updateRate" method="post">
                <input type="radio" name="rate" value = "0" %s> No reports<br>
                <input type="radio" name="rate" value = "1" %s> Every 5 minutes<br>
                <input type="radio" name="rate" value = "12" %s> Every 1 hour<br>
                <input type="radio" name="rate" value = "288" %s> Every day<br>
                <input type="submit" value="Update rate">
                </form></p></body></html>"""%(noReport, fiveMin, oneHour, oneDay))

class TrendingAPI(webapp2.RequestHandler):
	def post(self):
		updateViewCount()
		streams = Stream.query().order(-Stream.viewCount)
		result = []
		i = 0
		for stream in streams:
			if i < 3:
				if len(stream.photos) >= 1:
					result.append({stream.key.urlsafe(): [stream.name, stream.cover, str(stream.photos[len(stream.photos) - 1]), stream.viewCount]})
				else:
					result.append({stream.key.urlsafe(): [stream.name, stream.cover, 'no', stream.viewCount]})
			i += 1
		self.response.write(json.dumps(result))


class UpdateReportRate(webapp2.RequestHandler):
	global frequency, count
	def post(self):
		global frequency,count
		frequency = int(self.request.get("rate"))
		count = 0
		self.redirect("/trending")



def updateViewCount():
	timeNow = datetime.datetime.now()
	oneHour = datetime.timedelta(0,3600,0)
	timeCompare = timeNow - oneHour
	streams = Stream.query()
	for stream in streams:
		viewRecord = stream.viewRecord
		newRecord = []
		for time in viewRecord:
			if time > timeCompare:
				newRecord.append(time)
		stream.viewRecord = newRecord
		stream.put()
		stream.viewCount = len(newRecord)
		stream.put()


def jsonfyFetch(params, url):
	payload = json.dumps(params)
	result = urlfetch.fetch(url, payload = payload, method = urlfetch.POST).content
	result = json.loads(result)
	return result


class Error(webapp2.RequestHandler):
	def get(self):
		self.response.write("""<!doctype html><html><body>
			The name for stream is not available as it has been used.""")
		self.response.write("""<form action="/"><input type = "submit"
		 name = "return" value = "Return to main"></form></body></html>""")

class Social(webapp2.RequestHandler):
	def get(self):
		self.response.write("""<!doctype html><html><body>
			The page hasn't been implemented yet.""")
		self.response.write("""<form action="/"><input type = "submit"
		 name = "return" value = "Return to main"></form></body></html>""")



class CronJob(webapp2.RequestHandler):
	def get(self):
		global frequency, count
		updateViewCount()
		if frequency != 0:
			count += 1
			if count == frequency:
				count = 0
				#send mail
				params = {}
				url = 'http://aptconnexus11.appspot.com/trendingAPI'
				result = jsonfyFetch(params, url)
				name = ''
				for stream in result:
					streamKey = stream.keys()[0]
					name += stream[streamKey][0]
					name +=' '

				mail.send_mail(sender="<vinckyxie@gmail.com>",
                      to="<vinckyxie@gmail.com>,<zhang.yan.thu09@gmail.com>,<natviv@gmail.com>,<adnan.aziz@gmail.com>,<ragha@utexas.edu>",
                      subject="APT Connexus Trending",
                      body="""This is from APT miniproject, Connexus.http://wenjingxie1121.appspot.com/. Team members are Wenjing Xie and Yan Zhang.
                      The top 3 trending streams now are %s."""%name)

application = webapp2.WSGIApplication([
    ('/', MainPage), ('/manageAPI', ManageAPI), ('/deleteOwnStream', DeleteOwnStream), ('/deleteAPI', DeleteAPI),('/unsubscribe', Unsubscribe),
    ('/create', Create), ('/createSubmit', CreateSubmit), ('/createSubmitAPI', CreateSubmitAPI), 
    ('/viewAll', ViewAll), ('/viewAllAPI', ViewAllAPI),
    ('/getUrl', ImageUrlAPI),('/upload([^/]+)?', UploadHandler),('/subscribe/([^/]+)?/([^/]+)?', Subscribe), ('/subscribeAPI', SubscribeAPI),
    ('/search', Search), ('/searchSubmit', SearchSubmit), ('/searchAPI', SearchAPI),
    ('/trending', Trending), ('/trendingAPI', TrendingAPI), ('/updateRate', UpdateReportRate),
    ('/tasks/summary', CronJob),('/error', Error),('/social', Social),('/autocompleteAPI([^/]+)?', autocompleteAPI), ('/tasks/auto', autoDataAPI)
], debug=True)