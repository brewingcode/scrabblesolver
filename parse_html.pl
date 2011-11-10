#!/usr/bin/perl

use strict;
use warnings;

# dictionary of scrabble words available here:
# wget 'http://www.becomeawordgameexpert.com/dictionary.htm'

while (<>) {
    if (/<font face="Courier New">(.*)<\/font>/) {
        my @words = grep { length($_) > 1 } split / /, $1;
        print join "\n", map { lc($_) } @words;
    }
}
        
