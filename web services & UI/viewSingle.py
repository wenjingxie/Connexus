# -*- coding: utf-8 -*-
from __future__ import with_statement
import os
import urllib
import json
import re
import datetime
import webapp2
import random

from connexus import Stream
from google.appengine.api import files, images
from google.appengine.ext import blobstore, deferred
from google.appengine.api import users
from google.appengine.api import images
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import urlfetch
from google.appengine.ext import blobstore
from google.appengine.ext.webapp import blobstore_handlers

MIN_FILE_SIZE = 1  # bytes
MAX_FILE_SIZE = 5000000  # bytes
IMAGE_TYPES = re.compile('image/(gif|p?jpeg|(x-)?png)')
ACCEPT_FILE_TYPES = IMAGE_TYPES
THUMBNAIL_MODIFICATOR = '=s80'  # max width / height
EXPIRATION_TIME = 300000
StreamKeyUrl = ''
count = 0



class ViewSingle(webapp2.RequestHandler):
    def get(self, streamKey, imageKey, viewCount):
        streamKeyUrl = str(urllib.unquote(streamKey))
        streamKey = ndb.Key(urlsafe=streamKeyUrl)
        imageKeyStr = str(urllib.unquote(imageKey))
        imageKey = blobstore.BlobKey(imageKeyStr)
        stream = streamKey.get()
        images = stream.photos
        viewCount = str(urllib.unquote(viewCount))
        self.response.write("""<!DOCTYPE HTML>
<html lang="en">
<head>
<link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
<link rel="stylesheet" href="http://aptconnexus11.appspot.com/file/css/style.css">
<link rel="stylesheet" href="//blueimp.github.io/Gallery/css/blueimp-gallery.min.css">
<link rel="stylesheet" href="http://aptconnexus11.appspot.com/file/css/jquery.fileupload.css">
<link rel="stylesheet" href="http://aptconnexus11.appspot.com/file/css/jquery.fileupload-ui.css">
<noscript><link rel="stylesheet" href="http://aptconnexus11.appspot.com/file/css/jquery.fileupload-noscript.css"></noscript>
<noscript><link rel="stylesheet" href="http://aptconnexus11.appspot.com/file/css/jquery.fileupload-ui-noscript.css"></noscript>
</head>
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
 		# present three pictures and the button "more pics"
        if len(images) == 0:
            self.response.write("<h3> No pictures in this stream yet!</h3>")
        else:
            self.response.write("<p>")
            index = images.index(imageKey)
            if (index + 1) <= 3:
                while index >= 0:
                    blobkey = images[index]
                    params = {'blobKey': str(blobkey)}
                    url = 'http://aptconnexus11.appspot.com/getUrl'
                    imageUrl = jsonfyFetch(params, url)['url']
                    self.response.write('<img src = "%s" style="width:304px;height:228px">'%(imageUrl))
                    index -= 1
                self.response.write('No more pics!')
                self.response.write("</p>")
            else:
                for i in range(0, 3):
                    blobkey = images[index - i]
                    params = {'blobKey': str(blobkey)}
                    url = 'http://aptconnexus11.appspot.com/getUrl'
                    imageUrl = jsonfyFetch(params, url)['url']
                    self.response.write('<img src = "%s" style="width:304px;height:228px">'%(imageUrl))
                nextImageKey = str(images[index - 3])
                self.response.write("""<a href = "/viewSingle/%s/%s/0"> <form action="">
                          <input type="button" value="More pictures">
                          </form></a></p>"""%(streamKeyUrl, nextImageKey))

 
  #       # subscribe
        self.response.write("""<p><a href = "/subscribe/%s/%s"> <form action="">
                           <input type="button" value="Subscribe">
                           </form></a></p>
                           <p><a href="/geoView/%s"><form action="">
                           <input type="button" value="Geo view">
                           </form></a></p>"""%(streamKeyUrl, imageKeyStr,streamKeyUrl))
        self.response.write("""
                    <a href = "/reload/%s"> <form action="">
                          <input type="button" value="Reload this page!">
                          </form></a></p>"""%(streamKeyUrl))
        url = '/imgUploadHandler/%s'%streamKeyUrl
        self.response.write("""
<div class="container">
    <form id="fileupload" action="%s" method="POST" enctype="multipart/form-data">
    """%url)
        self.response.write("""
        <noscript><input type="hidden" name="redirect" value="https://blueimp.github.io/jQuery-File-Upload/"></noscript>
        <div class="row fileupload-buttonbar">
            <div class="col-lg-7">
                <span class="btn btn-success fileinput-button">
                    <i class="glyphicon glyphicon-plus"></i>
                    <span>Add files...</span>
                    <input type="file" name="files[]" multiple>
                </span>
                <button type="submit" class="btn btn-primary start">
                    <i class="glyphicon glyphicon-upload"></i>
                    <span>Start upload</span>
                </button>
                <button type="reset" class="btn btn-warning cancel">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <span>Cancel upload</span>
                </button>
                <button type="button" class="btn btn-danger delete">
                    <i class="glyphicon glyphicon-trash"></i>
                    <span>Delete</span>
                </button>
                <input type="checkbox" class="toggle">
                <span class="fileupload-process"></span>
            </div>
            <div class="col-lg-5 fileupload-progress fade">
                <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100">
                    <div class="progress-bar progress-bar-success" style="width:0%;"></div>
                </div>
                <div class="progress-extended">&nbsp;</div>
            </div>
        </div>
        <table role="presentation" class="table table-striped"><tbody class="files"></tbody></table>
    </form>
    <br>
</div>
<div id="blueimp-gallery" class="blueimp-gallery blueimp-gallery-controls" data-filter=":even">
    <div class="slides"></div>
    <h3 class="title"></h3>
    <a class="prev">‹</a>
    <a class="next">›</a>
    <a class="close">×</a>
    <a class="play-pause"></a>
    <ol class="indicator"></ol>
</div>
<script id="template-upload" type="text/x-tmpl">
{% for (var i=0, file; file=o.files[i]; i++) { %}
    <tr class="template-upload fade">
        <td>
            <span class="preview"></span>
        </td>
        <td>
            <p class="name">{%=file.name%}</p>
            <strong class="error text-danger"></strong>
        </td>
        <td>
            <p class="size">Processing...</p>
            <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0"><div class="progress-bar progress-bar-success" style="width:0%;"></div></div>
        </td>
        <td>
            {% if (!i && !o.options.autoUpload) { %}
                <button class="btn btn-primary start" disabled>
                    <i class="glyphicon glyphicon-upload"></i>
                    <span>Start</span>
                </button>
            {% } %}
            {% if (!i) { %}
                <button class="btn btn-warning cancel">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <span>Cancel</span>
                </button>
            {% } %}
        </td>
    </tr>
{% } %}
</script>
<script id="template-download" type="text/x-tmpl">
{% for (var i=0, file; file=o.files[i]; i++) { %}
    <tr class="template-download fade">
        <td>
            <span class="preview">
                {% if (file.thumbnailUrl) { %}
                    <a href="{%=file.url%}" title="{%=file.name%}" download="{%=file.name%}" data-gallery><img src="{%=file.thumbnailUrl%}"></a>
                {% } %}
            </span>
        </td>
        <td>
            <p class="name">
                {% if (file.url) { %}
                    <a href="{%=file.url%}" title="{%=file.name%}" download="{%=file.name%}" {%=file.thumbnailUrl?'data-gallery':''%}>{%=file.name%}</a>
                {% } else { %}
                    <span>{%=file.name%}</span>
                {% } %}
            </p>
            {% if (file.error) { %}
                <div><span class="label label-danger">Error</span> {%=file.error%}</div>
            {% } %}
        </td>
        <td>
            <span class="size">{%=o.formatFileSize(file.size)%}</span>
        </td>
        <td>
            {% if (file.deleteUrl) { %}
                <button class="btn btn-danger delete" data-type="{%=file.deleteType%}" data-url="{%=file.deleteUrl%}"{% if (file.deleteWithCredentials) { %} data-xhr-fields='{"withCredentials":true}'{% } %}>
                    <i class="glyphicon glyphicon-trash"></i>
                    <span>Delete</span>
                </button>
                <input type="checkbox" name="delete" value="1" class="toggle">
            {% } else { %}
                <button class="btn btn-warning cancel">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <span>Cancel</span>
                </button>
            {% } %}
        </td>
    </tr>
{% } %}
</script>
<script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/vendor/jquery.ui.widget.js"></script>
<script src="//blueimp.github.io/JavaScript-Templates/js/tmpl.min.js"></script>
<script src="//blueimp.github.io/JavaScript-Load-Image/js/load-image.all.min.js"></script>
<script src="//blueimp.github.io/JavaScript-Canvas-to-Blob/js/canvas-to-blob.min.js"></script>
<script src="//netdna.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>
<script src="//blueimp.github.io/Gallery/js/jquery.blueimp-gallery.min.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.iframe-transport.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.fileupload.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.fileupload-process.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.fileupload-image.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.fileupload-validate.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/jquery.fileupload-ui.js"></script>
<script src="http://aptconnexus11.appspot.com/file/js/main.js"></script>
</body> 
</html>""")

        if viewCount == '1':
            stream.viewRecord.append(datetime.datetime.now())
            stream.put()





class ImgUploadHandler(webapp2.RequestHandler):

    def initialize(self, request, response):
        super(ImgUploadHandler, self).initialize(request, response)
        self.response.headers['Access-Control-Allow-Origin'] = '*'
        self.response.headers[
            'Access-Control-Allow-Methods'
        ] = 'OPTIONS, HEAD, GET, POST, PUT, DELETE'
        self.response.headers[
            'Access-Control-Allow-Headers'
        ] = 'Content-Type, Content-Range, Content-Disposition'

    def validate(self, file):
        if file['size'] < MIN_FILE_SIZE:
            file['error'] = 'File is too small'
        elif file['size'] > MAX_FILE_SIZE:
            file['error'] = 'File is too big'
        elif not ACCEPT_FILE_TYPES.match(file['type']):
            file['error'] = 'Filetype not allowed'
        else:
            return True
        return False

    def get_file_size(self, file):
        file.seek(0, 2)  # Seek to the end of the file
        size = file.tell()  # Get the position of EOF
        file.seek(0)  # Reset the file position to the beginning
        return size

    def write_blob(self, data, info):
        blob = files.blobstore.create(
            mime_type=info['type'],
            _blobinfo_uploaded_filename=info['name']
        )
        with files.open(blob, 'a') as f:
            f.write(data)
        files.finalize(blob)
        return files.blobstore.get_blob_key(blob)

    def handle_upload(self,streamKeyUrl):
        streamKey = ndb.Key(urlsafe=streamKeyUrl)
        stream = streamKey.get()
        results = []
        blob_keys = []
        for name, fieldStorage in self.request.POST.items():
            if type(fieldStorage) is unicode:
                continue
            result = {}
            result['name'] = re.sub(
                r'^.*\\',
                '',
                fieldStorage.filename
            )
            result['type'] = fieldStorage.type
            result['size'] = self.get_file_size(fieldStorage.file)
            if self.validate(result):
                imgKey = self.write_blob(fieldStorage.value, result)
                blob_key = str(imgKey)
                blob_keys.append(blob_key)
                stream.photos.append(imgKey)
                geo = dict()
                geo['latitude'] = 30 * random.random()
                geo['longitude'] = 40 * random.random()
                print "gei", geo
                stream.geo.append(json.dumps(geo))
                stream.put()
                result['deleteType'] = 'DELETE'
                result['deleteUrl'] = 'http://aptconnexus11.appspot.com/imgUploadHandler' +\
                    '/?key=' + urllib.quote(blob_key, '')
                if (IMAGE_TYPES.match(result['type'])):
                    try:
                        result['url'] = images.get_serving_url(
                            blob_key,
                            secure_url=self.request.host_url.startswith(
                                'https'
                            )
                        )
                        result['thumbnailUrl'] = result['url'] +\
                            THUMBNAIL_MODIFICATOR
                    except:  # Could not get an image serving url
                        pass
                if not 'url' in result:
                    result['url'] = self.request.host_url +\
                        '/' + blob_key + '/' + urllib.quote(
                            result['name'].encode('utf-8'), '')
                results.append(result)
        return  results

   


    def post(self,streamKeyUrl):
        global StreamKeyUrl
        StreamKeyUrl = streamKeyUrl
        if (self.request.get('_method') == 'DELETE'):
            return self.delete()
        result = {'files': self.handle_upload(streamKeyUrl)}
        s = json.dumps(result, separators=(',', ':'))
        redirect = self.request.get('redirect')
        if redirect:
            return self.redirect(str(
                redirect.replace('%s', urllib.quote(s, ''), 1)
            ))
        if 'application/json' in self.request.headers.get('Accept'):
            self.response.headers['Content-Type'] = 'application/json'
        self.response.write(s)

    def deleteFromStream(self, blobKey):
        global StreamKeyUrl
        streamKey = ndb.Key(urlsafe=StreamKeyUrl)
        stream = streamKey.get()
        stream.photos.remove(blobKey)
        stream.put()


    def delete(self, resource):
        key = self.request.get('key') or ''
        blobKey = blobstore.BlobKey(key)
        self.deleteFromStream(blobKey)
        blobstore.delete(key)
        s = json.dumps({key: True}, separators=(',', ':'))
        if 'application/json' in self.request.headers.get('Accept'):
            self.response.headers['Content-Type'] = 'application/json'
        self.response.write(s)


class GetNewImgAPI(webapp2.RequestHandler):

    def post(self):
        params = json.loads(self.request.body)
        streamKeyUrl = params['key']
        streamkey = ndb.Key(urlsafe=streamKeyUrl)
        stream = streamkey.get()
        imgs = stream.photos
        if len(imgs) == 0:
            self.response.write(json.dumps({'imgKey': "no"}))
        else:
            imgKey = str(imgs[-1])
            print "img",imgKey
            self.response.write(json.dumps({'imgKey': imgKey}))

class ReloadViewSingle(webapp2.RequestHandler):
    def get(self, streamKeyUrl):
        print "reload"
        params = {"key":streamKeyUrl}
        url = "http://aptconnexus11.appspot.com/getNewImg"
        result = jsonfyFetch(params,url)
        imgKey = result['imgKey']
        self.redirect('/viewSingle/%s/%s/0'%(streamKeyUrl, imgKey))
        

def jsonfyFetch(params, url):
    payload = json.dumps(params)
    result = urlfetch.fetch(url, payload = payload, method = urlfetch.POST).content
    result = json.loads(result)
    return result


application = webapp2.WSGIApplication([
    ('/viewSingle/([^/]+)?/([^/]+)?/([^/]+)?', ViewSingle),
    ('/imgUploadHandler/([^/]+)?', ImgUploadHandler),
    ('/getNewImg', GetNewImgAPI),('/reload/([^/]+)?',ReloadViewSingle)
], debug=True)