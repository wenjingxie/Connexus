# -*- coding: utf-8 -*-
from __future__ import with_statement
import os
import urllib
import json
import re
import datetime
import webapp2

from connexus import Stream, jsonfyFetch
from google.appengine.api import files, images
from google.appengine.ext import blobstore, deferred
from google.appengine.api import users
from google.appengine.api import images
from google.appengine.ext import ndb
from google.appengine.api import mail
from google.appengine.api import urlfetch
from google.appengine.ext import blobstore
from google.appengine.ext.webapp import blobstore_handlers


class GeoView(webapp2.RequestHandler):

	def get(self,streamKeyUrl):
		streamKeyUrl = str(urllib.unquote(streamKeyUrl))
		self.response.write("""<!DOCTYPE HTML><html><head>
    		<link rel="stylesheet" href="http://aptconnexus11.appspot.com/geo/iThing.css" type="text/css" />
			<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.7/jquery.min.js"></script>
			<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=true"></script>
			<script type="text/javascript" src="http://aptconnexus11.appspot.com/geo/markerclusterer.js"></script>
			<script type="text/javascript" src="http://aptconnexus11.appspot.com/geo/jquery.ui.map.js"></script>
			<script type="text/javascript" src="http://aptconnexus11.appspot.com/geo/min/jquery.ui.map.full.min.js"></script>
            <script type="text/javascript" src="http://aptconnexus11.appspot.com/geo/jquery-ui-1.8.16.custom.min.js"></script>
      		<script type="text/javascript" src="http://aptconnexus11.appspot.com/geo/jQDateRangeSlider-min.js"></script>

			<script type="text/javascript">
function drawMap(){
			$('#map_canvas').gmap({'callback':function() {
        var self = this;
        var minDate = dateToInt($('#slider').dateRangeSlider('min'));
        var maxDate = dateToInt($('#slider').dateRangeSlider('max'));
        $.getJSON('http://aptconnexus11.appspot.com/geoViewAPI/%s', function(data) { 
                $.each( data.markers, function(i, m) {
                	if(m.timestamp >= minDate && m.timestamp <= maxDate){
                self.addMarker( { 'position': new google.maps.LatLng(m.latitude, m.longitude), 'bounds':true }).mouseover(function() {
                self.openInfoWindow({ 'content': m.content }, this)});
                	};

                });
		 self.set('MarkerClusterer', new MarkerClusterer(self.get('map'), self.get('markers'))); 
        });                                                                                                                                                                                                                             
  }});
}

function dateToInt(dateObject){
        var year  = dateObject.getFullYear();
        var month = dateObject.getMonth() + 1; // Returns the month (from 0-11)
        var day   = dateObject.getDate();
        var dateInt = (year * 10000) + (month * 100) + (day);
        return dateInt;
      }
$(function() {
	var d = new Date();
	var year  = d.getFullYear();
    var month = d.getMonth(); // Returns the month (from 0-11)
    var day   = d.getDate();
      $("#slider").dateRangeSlider({
      	bounds:{min: new Date(year - 1, month, day), max: new Date(year, month, day)},
      	defaultValues:{min: new Date(year - 1, month, day), max: new Date(year, month, day)},
      	step:{days:1}
      	});
       drawMap();
      $("#slider").bind("userValuesChanged", function(e, data){
      	console.log("change");
      	var cluster = $('#map_canvas').gmap('get', 'MarkerClusterer');
      	cluster.clearMarkers();
      	$('#map_canvas').gmap('clear', 'markers');

      	var minData = dateToInt(data.values.min);
      	var maxData = dateToInt(data.values.max);

      	$.getJSON('http://aptconnexus11.appspot.com/geoViewAPI/%s', function(images) {
                $.each( images.markers, function(i, m) {
                	if(m.timestamp >= minData && m.timestamp <= maxData){
                    var $newMarker = $('#map_canvas').gmap('addMarker', { 'position': new google.maps.LatLng(m.latitude, m.longitude), 'bounds':true });
                    $newMarker.mouseover(function() {
                    $('#map_canvas').gmap('openInfoWindow',{ 'content': m.content }, this)});
                	};

                });
		 $('#map_canvas').gmap('set', 'MarkerClusterer', new MarkerClusterer($('#map_canvas').gmap('get','map'), $('#map_canvas').gmap('get', 'markers'))); 
        }); 
      });
});
		</script>
		</head>
		<h1> Connex.us </h1>
         		<p>
         		<a href = "/"> Manage </a>
         		<a href = "/create"> | Create </a>
         		<a href = "/viewAll"> | View </a>
         		<a href = "/search"> | Search </a>
         		<a href = "/trending"> | Trending </a>
         		<a href = "/social"> | Social </a>
         		</p>
		<div id="map_canvas" style="width:1000px;height:400px"></div> 
		<div id="slider"></div>
			"""%(streamKeyUrl,streamKeyUrl))

class GeoViewApi(webapp2.RequestHandler):

    def get(self, streamKeyUrl):
        params = self.request.get('date')
        print "hello",params
        streamKeyUrl = str(urllib.unquote(streamKeyUrl))
        print streamKeyUrl
        streamKey = ndb.Key(urlsafe=streamKeyUrl)
        stream = streamKey.get()
        images = stream.photos
        geos = stream.geo
        infos = []
        for i in range(0, len(images)):
            info = dict()
            geo = json.loads(geos[i])
            info['latitude'] = geo['latitude']
            info['longitude'] = geo['longitude']
            blobkey = images[i]
            datetime =blobstore.BlobInfo.get(blobkey).creation.date()
            date = str(datetime).split('-')
            info['timestamp'] = int(date[0])*10000 + int(date[1])*100 + int(date[2])
            params = {'blobKey': str(blobkey)}
            url = 'http://aptconnexus11.appspot.com/getUrl'
            imageUrl = jsonfyFetch(params, url)['url']
            info['content'] = '<img src=%s style="width:100px;height:100px">'%imageUrl
            infos.append(info)
        result = {"markers":infos}
        result1 = {"markers":[ { "latitude":20, "longitude":30, "timestamp":20141018, "content":"Representing :)" }, { "latitude":21, "longitude":30, "timestamp":20141018, "content":"Swedens second largest city" } ]}
        self.response.write(json.dumps(result))



application = webapp2.WSGIApplication([
    ('/geoView/([^/]+)?', GeoView),
    ('/geoViewAPI/([^/]+)?', GeoViewApi)
], debug=True)