use strict;
use feature qw(fc);

our $fields;
our $count=0;

our $filerows;
our $prevFileRow='';
our $glossbody; 
our $glossentry;
our $switch=0;

our @fileline;
our @lines;


sub R35_Write_Body{
		
	print OUTPUT "$glossbody\n";
	
}


sub R30_Init_Body{
    my $glossbodyTag='<glossBody><glossAlt><glossAcronym>';
    my $glossbodyTagClose='</glossAcronym></glossAlt></glossBody>';
    
	$glossbody="\x20 $glossbodyTag$fileline[2]$glossbodyTagClose";
	
	# set switch back to avoid <glossbody>
	$switch=0;
	
}




sub R29_Fin_Line{
	
   print OUTPUT "</glossentry> \n \n";
   
}


sub R25_Write_Line{

    # Write line to output file
    print OUTPUT "$glossentry\n";
}


sub R20_Init_Line{
	my $glossentryTag;
    my $glosstermTag;
    my $glossdefTag;
    my $termID;
    my $ID='';
    my $dQuote='"';
	
	# determine whether the line contains an acronym for glossbody and makes switch
	# fills the field
	
	$fields = () = $filerows =~ /\|/g;
	
	# split line
	@fileline=split /\|/,$filerows,3;
	
	# Build  glossentry ID from glossterm, remove special characters and spaces
    $termID=$fileline[0];
    $termID=~s/[^a-zA-Z0-9]*//g;

    if($termID eq $prevFileRow) {
	  $count=$count+1;
      $ID=join '_', $termID, "$count";
    }else{
      $ID=$termID;
      $count=0;
    }
    $glossentryTag='<glossentry '."id=$dQuote$ID$dQuote> \n";
    $glosstermTag="\x20 <glossterm>$fileline[0]</glossterm> \n";
    $glossdefTag="\x20 <glossdef>$fileline[1]</glossdef>";
    
    $glossentry=$glossentryTag.$glosstermTag.$glossdefTag;
    
    # if the next entry is the same, the routine adds _ and a serial number
    $prevFileRow=$ID;
    
    #set switch for <>glossbody> tag
    if ($fileline[2] gt "\x20"){
    	$switch=1;
    }
}


sub R19_Fin_File{
    # Write closing tag
      print OUTPUT '</glossgroup>';
    # Close output file
    close(OUTPUT);
}


sub R10_Init_File{
	my $header='<?xml version="1.0" encoding="UTF-8"?>'."\n".
    '<!DOCTYPE glossgroup PUBLIC "-//OASIS//DTD DITA Glossary Group//EN" "glossgroup.dtd">'."\n".
    '<glossgroup id="glossgroup_glossary">'."\n".
    "<title>Glossary</title> \n";
    
    # sort input
    @lines=sort{fc($a) cmp fc($b)} @lines;
    
    # open output file and fill with header
    open(OUTPUT, '>:encoding(UTF-8)' , "glossary.dita") or die "Error OUTPUT FILE Couldn't open file, $!";
    print OUTPUT $header;
}


sub R09_Fin_Prog{
    # Close input file
    close(INPUT), or die "ERROR: FILE Couldn't close  file, $!";;
}


sub R01_Init_Prog{
	
    # Open input file for read
    open(INPUT,"<", "input.csv") or die "ERROR INPUT FILE Couldn't open file, $!";
    R05_Read_Lines();
    $filerows=scalar @lines;
}


sub R05_Read_Lines{
    # Read input file
    @lines=<INPUT>;  
}


sub R00_Main{
    R01_Init_Prog();
    if ( $filerows > 0 ){
      R10_Init_File();
      foreach $filerows (@lines){
      	R20_Init_Line();
        R25_Write_Line();
        
        if ($switch > 0){
        	R30_Init_Body;
        	R35_Write_Body;
        }
       R29_Fin_Line(); 
      }
    R19_Fin_File();
    }
    R09_Fin_Prog();
}


R00_Main()
