<?PHP

/*

'###########################################################################
'#
'# Example PHP Script for Loading Save RAM files into JavaBoy applet
'# Based on the example for NESCafe by David de Niese, ported to PHP
'# by Daniel Fisher.
'#
'# @authors : Daniel Fisher     http://www.everyvideogame.com
'#          : David de Niese    http://www.davieboy.net
'#          : Modified by Neil Millstone    http://www.millstone.demon.co.uk
'#
'# @version : 0.58f
'#
'# The Save RAM files are to be located from a subdirectory called saveram
'#
'# Use:
'# <param name=LOADRAMURL value="http://domain/loadram.php?user=user">
'#
'###########################################################################

*/


// Prevent Cache

	header('Expires: Mon, 26 Jul 1997 05:00:00 GMT'); 
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
	


// Check File Exists

	if (file_exists ("saveram/".$user."-".$gamename.".sav")) 
	{

		// Get Data Into Array

			$lines = file("saveram/".$user."-".$gamename.".sav");



		// Prevent HTML Header Being Sent for Blank Lines

			if (!$lines)
			{
				echo "\r\n";
			}


		// Echo the Save Information

			foreach ($lines as $line_num => $line)
			{
				echo "{$line}\r\n";
			}


	} 
	else 
	{

		echo "NOSAVERAM\r\n";

	}


// Clean Exit

	exit;

?>