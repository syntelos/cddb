Status

  Work in progress

About

  A simple bit of java for renaming (and tagging) music files using track names found via musicbrainz.  See "src/cddb/Main.java" and "src/cddb/API.java" for an intro to musicbrainz services.

Use

  Compile using 'mk.sh' and run using 'java-jar cddb.jar <dir>', where <dir> is something like "~/Music/ZZ Top/Greatest Hits/".  This renames "Track #.wav" files to "##. <track name>.wav" and adds ID3 tags.  Useful on linux.  A tagging program might be more fun.

Experience

  From linux WAV files, convert to MP3 and tag with ID3 for Rhythmbox.

Author

  John Pritchard
  mailto:jdp@syntelos.org
  http://www.syntelos.org/

