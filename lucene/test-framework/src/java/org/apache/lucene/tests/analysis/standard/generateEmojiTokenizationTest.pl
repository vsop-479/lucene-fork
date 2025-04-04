#!/usr/bin/perl

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

use warnings;
use strict;
use File::Spec;
use Getopt::Long;
use LWP::UserAgent;

my ($volume, $directory, $script_name) = File::Spec->splitpath($0);

my $version = '';
unless (GetOptions("version=s" => \$version) && $version =~ /\d+\.\d+/) {
    print STDERR "Usage: $script_name -v <version>\n";
    print STDERR "\tversion must be of the form X.Y, e.g. 11.0\n"
        if ($version);
    exit 1;
}
my $url = "http://www.unicode.org/Public/emoji/${version}/emoji-test.txt";
my $underscore_version = $version;
$underscore_version =~ s/\./_/g;
my $class_name = "EmojiTokenizationTestUnicode_${underscore_version}";
my $output_filename = "${class_name}.java";
my $header =<<"__HEADER__";
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.tests.analysis.standard;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.tests.analysis.BaseTokenStreamTestCase;

/**
 * This class was automatically generated by ${script_name}.
 * from: <a href="${url}"><code>${url}</code></a>
 */
public final class ${class_name} {

  public static void test(Analyzer analyzer) throws Exception {
    for (int i = 0 ; i < TESTS.length ; i += 2) {
      String test = TESTS[i + 1];
      try {
        BaseTokenStreamTestCase.assertAnalyzesTo(analyzer, test, new String[] { test }, new String[] { "<EMOJI>" });
      } catch (Throwable t) {
        throw new Exception("Failed to tokenize \\"" + TESTS[i] + "\\":", t);
      }
    }
  }

  private static final String[] TESTS = new String[] {
__HEADER__

my @tests = split /\r?\n/, get_URL_content($url);

my $output_path = File::Spec->catpath($volume, $directory, $output_filename);
open OUT, ">$output_path"
    || die "Error opening '$output_path' for writing: $!";

print STDERR "Writing '$output_path'...";

print OUT $header;

my $isFirst = 1;
for my $line (@tests) {
    next if ($line =~ /^\s*(?:|\#.*)$/); # Skip blank or comment-only lines

    print OUT ",\n\n" unless $isFirst;
    $isFirst = 0;

    # Example line: 1F46E 1F3FB 200D 2642 FE0F                 ; fully-qualified     # 👮🏻‍♂️ man police officer: light skin tone
    $line =~ s/\s+$//;     # Trim trailing whitespace
    $line =~ s/\t/  /g; # Convert tabs to two spaces (no tabs allowed in Lucene source)
    print OUT "    \"$line\",\n";
    my ($test_string) = $line =~ /^(.*?)\s*;/;
    $test_string =~ s/([0-9A-F]+)/\\u$1/g;
    $test_string =~ s/\\u([0-9A-F]{5,})/join('', map { "\\u$_" } above_BMP_char_to_surrogates($1))/ge;
    $test_string =~ s/\s//g;
    print OUT "    \"${test_string}\"";
}
print OUT "  };\n}\n";
close OUT;
print STDERR "done.\n";


# sub above_BMP_char_to_surrogates
#
# Converts hex references to chars above the BMP (i.e., greater than 0xFFFF)
# to the corresponding UTF-16 surrogate pair
#
# Assumption: input string is a sequence more than four hex digits
#
sub above_BMP_char_to_surrogates {
    my $ch = hex(shift);
    my $high_surrogate = 0xD800 + (($ch - 0x10000) >> 10);
    my $low_surrogate  = 0xDC00 + ($ch & 0x3FF);
    return map { sprintf("%04X", $_) } ($high_surrogate, $low_surrogate);
}


# sub get_URL_content
#
# Retrieves and returns the content of the given URL.
#
sub get_URL_content {
    my $url = shift;
    print STDERR "Retrieving '$url'...";
    my $user_agent = LWP::UserAgent->new;
    my $request = HTTP::Request->new(GET => $url);
    my $response = $user_agent->request($request);
    unless ($response->is_success) {
        print STDERR "Failed to download '$url':\n\t",$response->status_line,"\n";
        exit 1;
    }
    print STDERR "done.\n";
    return $response->content;
}
