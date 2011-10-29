<?php

/*

'###########################################################################
'#
'# Example PHP Script for Saving Save RAM files into JavaBoy applet
'# Based on the example for NESCafe by David de Niese, ported to PHP
'# by Daniel Fisher.
'#
'# @authors : Daniel Fisher     http://www.everyvideogame.com
'#          : David de Niese    http://www.davieboy.net
'#          : Modified by Neil Millstone    http://www.millstone.demon.co.uk
'#
'# @version : 0.58f
'#
'# The SaveRAM files are to be stored to a subdirectory called saveram
'#
'# Use:
'# <param name=SAVERAMURL value="http://domain/saveram.php?user=user">
'#
'###########################################################################

*/


// Prevent Cache

	header('xpires: Mon, 26 Jul 1997 05:00:00 GMT'); 
	header('Cache-Control: no-store, no-cache, must-revalidate'); 
	header('Cache-Control: post-check=0, pre-check=0', FALSE); 
	header('Pragma: no-cache'); 


// Get the Username

	$user = "";

	if (isset($_GET['user']))
	{
		$user = $_GET['user'];
	}

	if ($user  == "") 
	{
		$user = "unknown";
	}


// Grab the Name of the Game Loaded into JavaBoy

	$gamename = "";

	if (isset($_POST['gamename']))
	{
		$gamename = $_POST['gamename'];
	}

	if ($gamename  == "")
	{

		// Game Name was not Posted

			$gamename = "unknown";

	}
	else
	{

		// Remove Extension

			//$gamename = substr($gamename, 0, -4); 



	}




// Grab the Number of Hex Values to Read from the Data Stream

	$dl = 0;
	if (isset($_POST["datalength"]))
	{
		$dl = $_POST["datalength"];
	}



// Declare Local Variables

	$i = 0;
	$write = "";
	$total = 0;


// Read Bank0

	$BankData = "";

	if (isset($_POST["data0"]))
	{
		$BankData = $_POST["data0"];
	}


	while($BankData != "") 
	{

		// Add Bank Data to Buffer

			$write .= $BankData;
			$write .= "\r\n";


		// Increment Bank Number

			$i++;


		// Increment Total Number of Bytes Read

			$total = $total + strlen($BankData);


		// Read from Next Bank

			$BankData = "";

			if (isset($_POST["data".$i]))
			{
				$BankData = $_POST["data".$i];
			}

	}



// Check Totals Match

	if ($dl != $total) 
	{

		echo "Data Receive Error  (" . $dl . " != " . $total . ")";
		exit;

	}




// Check that ScreenShot Directory Exists

	if (!is_dir("saveram"))
	{
		
		if (!mkdir ("saveram"))
		{
			echo "Failed to create directory";
			exit;
		}

	}



// Save RAM to File

	$fp = fopen("saveram/".$user."-".$gamename.".sav", "w");
	fwrite($fp, $write);
	fclose($fp);



// Declare Success

	echo "Success".$i."\r\n";

?>