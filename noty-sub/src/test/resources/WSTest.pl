#!/bin/env perl
#/appl/pm/vendor/perl/sol-sparc/perl-5.14.1/bin/perl

use warnings;
use strict;

use Data::Dumper;
use JSON;

require HTTP::Request;
require LWP::UserAgent;	# simulate browser

use Getopt::Long;

my ($user, $signinOps, $eventsOps, $usage);
GetOptions( 'user=s' => \$user,
	'signin=s' => \$signinOps,
	'events=s' => \$eventsOps,
	'usage', \$usage );

if( defined $usage ) {
	print <<EUSAGE;
<PintWebServiceTestScript> -user <user-name> -signinOps <open;close;> -events <add=json-file;search=json-file;> -usage
	-user : user name to be used in interaction with web service. Default: use 'WSTest'
	-signin : signin-Ops to be used in interaction with web service. Default: open;close;
	-events : File containing json format event search query. Default, there is a sample one used.
	-usage : show this help
EUSAGE

	exit 0;
}

$user = 'WSTest' if( not defined $user );
$signinOps = 'open;close;' if( not defined $signinOps );
$eventsOps = 'add=;search=;' if( not defined $eventsOps );

my $jsonH = JSON->new->allow_nonref;
my $browser = LWP::UserAgent->new;
$browser->cookie_jar( { file => 'cookie.txt', autosave => 1, ignore_discard => 1 } );

my $pintURL = "http://localhost:8090/pint";
my $sessionURL  = "/sessions/$user";
if( $signinOps =~ /open/ ) {
	my $sessReq = HTTP::Request->new( 'POST', $pintURL.$sessionURL );
	$sessReq->header( 'Content-Type' => 'application/json' );
	$sessReq->content( '{}' );	#empty content makes vertx stuck

	my $sessRsp = $browser->request( $sessReq );
	print "Open Session Resp:\n";
	print Dumper( $sessRsp );

	my $sessRspJ = $jsonH->decode( $sessRsp->content );
	print "Content: ", $jsonH->pretty->encode( $sessRspJ ), "\n";
}

if( $eventsOps =~ /add/ ) {
	my $addEventsURL = '/events';
	my $addFeed;
	my $eaddf = $eventsOps;
	$eaddf =~ /.*add=([^;]+);.*/;
	$eaddf = $1;

	if( defined $eaddf ) {
		open( my $EADDF, $eaddf ) or die "Couldnt open add query for reading: $!\n";
		while( <$EADDF> ) {
			chomp;
			$addFeed .= $_;
		}
		close( $EADDF );
		$addFeed = $jsonH->decode( $addFeed );
	}

	if( not defined $addFeed ) {
		print "Using Default Add Feed\n";
		$addFeed = {
		"eventSource" => {
	 "type" => "NCD", "body" => { "dbEnv" => "SYBBTAPIM"  , "ncEnv" => "BETA"  , "region" => "U"  , "dboEnv" => "PM" 	} },
		"events" => {
			 "cycleDate" => "Feb 10 2014 00:00:00",
			"body" => [ {  "eId" => { "ids" => [ "", "P4BO-000" ] },
			"eTypeInfo" => { "progName" => "poll_for_bo_cycle.csh",
				"desc" => "main            " },
			"eInfo" => { "hostname" => "betapm",
				"pid" => "1005" },
			"eTime" => "Feb 10 2014 23:56:31",
			"endTime" => "Feb 11 2014 03:20:55",
			"status" => "0" }
			]
		}
	  };
=pod
		$addFeed = {
			"eventSource" => { "dbEnv" => "SYBBTAPIM"  , "ncEnv" => "BETA"  , "region" => "U"  , "dboEnv" => "PM" 	}, "events" => {
			 "cycleDate" => "Feb 10 2014 00=>00=>00",
			"values" => [ {  "eId" => { "id" => "P4BO-000",
					"parentId" => "        " },
				"eTypeInfo" => { "progName" => "poll_for_bo_cycle.csh",
					"desc" => "main            " },
				"eInfo" => { "hostname" => "betapm",
					"pid" => "1005" },
				"startTime" => "Feb 10 2014 23=>56=>31",
				"endTime" => "Feb 11 2014 03=>20=>55",
				"statusCode" => "0" } ]
			}
		};
=cut
	}

	my $eaddReq = HTTP::Request->new( 'POST', $pintURL.$addEventsURL );
	$eaddReq->header( 'Content-Type' => 'application/json' );
	my $s = $jsonH->pretty->encode( $addFeed );
	$eaddReq->content( $s );
	my $eaddRsp = $browser->request( $eaddReq );
	print "Event Add Rsp:\n", Dumper( $eaddRsp );
}

if( $eventsOps =~ /search/ ) {
	my $searchEventsURL = '/events/search';
	my $searchQuery;
	my $esearchf = $eventsOps;
	$esearchf =~ /.*search=([^;]+);.*/;
	$esearchf = $1;

	if( defined $esearchf ) {
		open( my $ESEARCHF, $esearchf ) or die "Couldnt open search query for reading: $!\n";
		while( <$ESEARCHF> ) {
			chomp;
			$searchQuery .= $_;
		}
		close( $ESEARCHF );
		$searchQuery = $jsonH->decode( $searchQuery );
	}

	if( not defined $searchQuery ) {
		print "Using Default Search Query\n";
		$searchQuery = [ "EventCache", "findEvents", {
			"eventSource" => {
				 "type" => "NCD", "body" => { "dbEnv" => "SYBBTAPIM"  , "ncEnv" => "BETA"  , "region" => "U"  , "dboEnv" => "PM" 	} },
			"events" => {
				 "cycleDate" => "Feb 10 2014 00:00:00",
				"body" => "#cmpDate( #e.eTime, \"Feb 10 2014 02:11:16\") > 0" }
		} ];
=pod
		$searchQuery = {
			"body" => { "EventCache" =>
			{ "findEvents" => {
				"eventSource" => {
					"dbEnv" => "SYBBTAPIM", "ncEnv" => "BETA", "region" => "U", "dboEnv" => "PM" },
					"events" => {
						"cycleDate" => "Feb 10 2014 00:00:00",
						"body" => "#cmpDate( #e.startTime, \"Feb 10 2014 02:11:16\") > 0"
					}
			  }
			} }
		  };
=cut
	}

	my $esearchReq = HTTP::Request->new( 'POST', $pintURL.$searchEventsURL );
	$esearchReq->header( 'Content-Type' => 'application/json' );
	my $s = $jsonH->pretty->encode( $searchQuery );
	$esearchReq->content( $s );
	my $esearchRsp = $browser->request( $esearchReq );
	print "Event Search Rsp:\n", Dumper( $esearchRsp );
}

if( $signinOps =~ /close/ ) {
	my $sessionURL  = "/sessions/$user";
	my $sessReq = HTTP::Request->new( 'DELETE', $pintURL.$sessionURL );
	$sessReq->header( 'Content-Type' => 'application/json' );
	$sessReq->content( '{}' );

	my $sessRsp = $browser->request( $sessReq );
	print "DEL Session Resp:\n";
	print Dumper( $sessRsp );
}
