---

layout: post
title: String Algorithms Part 1 - Manacher's
tags: gradle, docker, automation, hacking

---
{% assign im = "<span class='render-math'>" %}
{% assign mi = "</span>" %}
{% assign dm = "<p style='text-align: center' class='render-math center'>" %}
{% assign md = "</p>" %}

Recently, I encountered a number of seemingly simple questions about
strings. For example, what is the runtime complexity of sorting an array
of strings. Intuitively, if <span class="render-math">n</span> is the 
number of elements in the array, then the optimal runtime complexity -- using
merge sort or heap sort -- should be <span class="render-math">O(n\log n)</span>, no?

Well, not quite. You probably don't need to think too much to realize
that it depends on the length of the strings in the array. If we are
sorting an array of small English words, that's going to have a
different runtime to sorting an array of large strings of DNA sequences
or Dickens novels. 

That and similar problems made me search my soul a bit. What other
string problems have I oversimplified in my mind?

Well in a series of blog entries, I want to discuss a few truly astounding 
"linear" results that I learned from some of the great cybertutors 
(references below), starting with the one that has the narrowest focus: 
Manacher's Algorithm. In fact, I have never seen a complete exposition
of Manacher's algorithm presented together with proof of its optimality.
I thought I'd be the documentarian of this clever algorithm; I find that
it's an effort well-spent.

In future parts, I would like to discuss Knuth-Morris-Pratt (Part 2), 
Aho-Corasick (Part 3), Compact Tries (Part 4) and Suffix
Trees (Part 5).

My hope is to pique the interests of algorithm enthusiasts to add a few
string-related tricks to their arsenal of cool algorithms.

## Longest Palindromic Substring Problem

A palindrome is a string that is identical when reversed. For example,
the name "BOB" is the same spelled backwards. Ignoring white-spaces,
phrases like "we panic in a pew" are often fun and famous examples that,
when set to music, may sound something like this: ["Bob".](https://www.youtube.com/watch?v=JUQDzj6R3p4)

Given a string `S`, we can ask: what is the longest palindromic substring?

As an example, the string `"abracarbrabaddabra"`, while not itself a 
palindrome, has a few palindromic substrings: `"bracarb"`" (length 7)
and `"baddab"` (length 6). What is an algorithm that finds the
longest such palindromic substring `"bracarb"`?

Let's start with the Brute-force approach: list out every substring
and check which one is the longest one. We begin by writing down a
function to check if a string is a palindrome. Here, we assume that 
`""` is a palindrome. 

(For the TDD folks, you might want to start with
some obvious test cases: `""`, `"a"`, `"abba"`, `"aba"` and `"abbb"`
and `"abb"`.)

```go
// IsPalindrome - Tests whether a string s is a palindrome
// or not
func IsPalindrome(s string) bool {
    sLen := len(s)
    for i := 0; i < sLen / 2; i++ {
        if s[i] != s[sLen - i - 1] {
            return false
        }
    }
    return true
}
```
To solve the problem, we can now approach it like this:

1. set aside a tracker for the longest palindrome substring seen
   so far `maxPalindrome` (optionally cache its length `maxLen`)
2. generate a list of all substrings of some `s`
3. iterate through each substring: if a substring is a palindrome 
   and is longer than the current maximum, replace the current 
   maximum with the substring (and update the maximal length)

(A few test cases might include: `""`, `"a"`, `"abba"`, `"acadab"`, 
`"abababcac"` -- multiple possible palindromes, `"abrasive"` -- multiple 
possible Palindrome.)

And the code might look something like this:

```go
func FindMaxPalindrome(s string) (maxPalindrome string, maxLen int) {
    for i := 0; i < len(s); i++ {
        for j > i; j < len(s); j++ {
            if IsPalindrome(s[i:j]) && j - i > maxLen {
                maxPalindrome = s[i:j]
                maxLen = j - i
            }
        }
    }
    return
}
```

The runtime complexity is <span class="render-math">O(n^3)</span> where 
<span class="render-math">n</span> is the length of the string. 

There are a few suboptimal steps in the computation. For example, if
the substring defined by `(i, j)` is not a palindrome, then the one defined
by `(i - 1, j + 1)` will not be a palindrome either. Furthermore, if
`(i, j)` defines a palindrome, then checking whether `(i - 1, j + 1)` defines
a palindrome requires only one comparison: `s[i - 1]` against `s[j + 1]`. 
These would cut down on the number of unnecessary comparisons. 

However, it is far from obvious that such simplifications -- if implemented
correctly -- would reduce the runtime from {{im}}O(n^3){{mi}} to 
{{im}}O(n^2){{mi}} (nevermind a linear runtime!).

## Manacher's Algorithm

Let's instead take a different approach. Let us create an auxiliary array
that tracks the size of the largest possible palindrome centered at each
character. As some palindromes may have even number of characters -- `"aa"`, say
-- and whose point of symmetry occurs between characters, we will use a special 
indexing trick to track these as well. Often the index tricks can be thought of
as inserting a special character `#` between each character, and at the front
and end of the string.

To help us visualize, consider the string in the last section `abracarbrabaddabra`.
Writing the array of maximal palindrome lengths `P` as numbers beneath the augmented
string, we arrive at the following values:
```
#a#b#r#a#c#a#r#b#r#a#b#a#d#d#a#b#r#a#
0101010107010105010103010161010101010
```
Going through the array, it is immediately obvious that the longest palindromic
substring is centered at `c` (index 4) with length 7. Given such an array, 
the runtime for finding the maximum length and retrieving the string value will be 
linear. The trick is then to find `P` in linear time -- and that is the crux of 
Manacher's algorithm.

The naive solution to find `P` is relatively simple to code up:
```go
// Assuming that s is already augmented with '#', computes
// an array of the largest Palindrome
func findPNaively(s string) []int {
    P := make([]int, len(s), len(s))
    for i, _ := range s {
        longestPalindrome := 1
        for j := 1; j <= i; j++ {
            if i + j >= len(s) {
                break
            }

            if s[i - j] == s[i + j] {
                longestPalindrome += 2
            } else {
                break
            }
        }

        // we are dividing by 2 to discount the '#' characters
        P[i] = longestPalindrome / 2
    }

    return P
}
```

It is easy to see that the naive algorithm runs in {{im}}O(n^2){{mi}}
time. (Consider, for example, the string `aaaaaaaaaaaa`.) Not great.

However, there is also quite a bit of waste. In the naive implementation, 
a palindrome's symmetry is ignored in `findPNaively` when we iterate through
each character of `s`. Let's see how we can better exploit the symmetry 
to reduce wasted lookups.

Consider the palindrome "acacdabadcaca". If you look carefully, there
are two "off-centered" palindromic substrings (both "cac"). The fact there
are 2 of the same value is not
a coincidence; the right "cac" is just a reflection of the
left one about the point of symmetry.

In fact, this is generally true: any "off-centered" palindromes 
contained entirely within another palindrome occur in pairs. This
is because of two facts: 

1. palindromes are, by definition, invariant under reversal and 
2. for any palindrome, the substring after the pivot point is equal 
   to the reverse of the substring before the pivot point

We might want to more formally state this insight in terms of
maximal palindrome and the array {{im}}P{{mi}} of maximal palindrome length:

> For nonnegative integers {{im}}i{{mi}} and {{im}}j{{mi}} where 
{{im}}i < j{{mi}}, if {{im}}i - P[i]/2 > j - P[j]/2{{mi}} then
{{im}}P[i] = P[2j - i]{{mi}}.

*Proof*: if {{im}}i - P[i] / 2 < j - P[j] / 2{{mi}} then the 
maximal palindrome centered at {{im}}i{{mi}} is contained entirely
within the palindrome centered at {{im}}j{{mi}}. Its reflection
about {{im}}j{{mi}} is another palindrome of exactly the same length.

To see this, if its reflection were longer by 2 characters, say,
then, since {{im}}i - P[i]/2 > j - P[j]/2{{mi}}, those two characters 
are also within the palindrome centered at {{im}}j{{mi}}, and whose
reflection about {{im}}j{{mi}} is a longer palindrome centered at 
{{im}}i{{mi}}, contradicting the maximality of {{im}}P[i]{{mi}}.
{{im}}\blacksquare{{mi}}

The above covers the case when one palindromic substring is contained 
entirely inside another. What about the case when two palindromic 
substrings only partially intersect? In this case, we derive the 
following results about the array {{im}}P{{mi}}:

> For nonnegative integers {{im}}i{{mi}} and {{im}}j{{mi}} where 
{{im}}i < j{{mi}}, if {{im}}i - P[i]/2 < j - P[j]/2{{mi}} then the
maximal palindrome centered at {{im}}2j - i{{mi}} is a suffix of the palindrome
centered at {{im}}j{{mi}}. In particular, {{im}}P[2j - i] = 2i + P[j] - 2j{{mi}}

*Proof*: That a suffix centered at {{im}}2j - i{{mi}} is a 
palindrome is clear: it is a reflection of a substring of the palindrome
centered at {{im}}i{{mi}} about {{im}}j{{mi}}.

If the maximal palindrome centered at {{im}}2j - i{{mi}} were any longer by 2
characters, say, then the character immediately before maximal palindrome 
centered at {{im}}j{{mi}} and the character immediately after would be the 
same, contradicting its maximality.
{{im}}\blacksquare{{mi}}

A "reflection" of the above result about {{im}}j{{mi}} gives the following:

> For nonnegative integers {{im}}i{{mi}} and {{im}}j{{mi}} where 
{{im}}i < j{{mi}}, if {{im}}i - P[i]/2 = j - P[j]/2{{mi}} then
the maximal palindrome centered at {{im}}2j - i{{mi}} extends to at least
the end of the palindrome centered at {{im}}j{{mi}}. That is,
{{im}}P[2j - i] \geq P[i]{{mi}}
{{im}}\blacksquare{{mi}}

To put it altogether, let us sketch out the algorithm for `FindPManacher`.
The idea is to iterate through each character to find the maximal palindrome 
centered at that character. We will leverage these three results to avoid
checking the maximal palindrome sizes when we already have information about
them.

The algorithm may be stated as follows:

Given a non-nil string `s`

1. Let `j` track the index of the center of the current largest palindrome.
   This value is initialised as 0 and will be updated when we start to evaluate
   indices outside of the maximal palindrome centered at `j` or if
   we encountered a palindrome that is longer than the one centered at `j`.
   
   Let `r` track the radius of the palindrome initialised as 0. Notice that 
   this is the same as `P[j]` because we are treating the augmented string `s`
   with `#` characters inserted. This value will be updated whenever we update `j`.
   We will include the variable in the verbal description of the algorithm, but
   omit this value from the code.

   Let `P` track the array of maximal palindrome lengths, initialised with
   0 with capacity and length equal to the length of `s`

2. For each `i`, do the following: 

   - if `i` is greater than `j + r`, then set
     `j = i` and find the longest palindrome centered at `j`; suppose its
     radius is `maxLen` then set `P[i] = maxLen` and set `r = maxLen`

   - otherwise, let `k` equal to the pivot of `i` about `j`. If 
     `k - P[k] < j - r` then set `P[i] = i + P[j] - j`. If
     `k - P[k] > j - r` then set `P[i] = P[k]`. Otherwise, find the 
     largest palindrome centered at `i` starting at position `k + r`. 
     Suppose its radius is `maxLen` then set `P[i] = maxLen`. 

     Update `j` and `r` if the palindrome centered at `i` extends beyond
     the palindrome centered at `j` -- because the former offers us more
     symmetry data than the latter.

3. Return the array `P`

In code, this looks something like:

```go
func pivot(i, center int) int {
    return 2 * center - i
}

func findMaxPalindromeLen(s string, center, rightStart int) int {
    right := rightStart
    for ; right < len(s); right++ {
        left := pivot(right, center)
        if left < 0 {
            break
        }

        if s[left] != s[right] {
            break
        }
    }

    return right - center - 1
}

// FindPManacher - for a given string s, find the array of
// maximal palindrome lengths where the value at position i
// is the length of a palinwith centered at i
func FindPManacher(s string) []int {
    // As defined above:
    // j is the index of the last largest palindrome
    // P is the array of maximal palindrome lengths
    j := 0
    P := make([]int, len(s), len(s))

    for i, _ := range s {
        if i > j + P[j] {
            maxLen := findMaxPalindromeLen(s, i, i)
            j = i
            P[i] = maxLen
            continue
        } 
        
        k := pivot(i, j) 
        if k - P[k] < j - P[j] {
            P[i] = i + P[j] - j
        } else if k - P[k] > j - P[j] {
            P[i] = P[k]
        } else {
            maxLen := findMaxPalindromeLen(s, i, j + P[j] + 1)
            P[i] = maxLen

	    // if the palindrome centered at i extends beyond 
	    // the boundary of that centered at j, then update j
            if i + P[i] > j + P[j] {
                j = i
            }
        }
    }
}
```

## Walk-through of an Example

Let us run through the code for an example string `"dadccdadccd"`. Recall 
that the string we will be operating on is actually augmented with
`#`; therefore, let `s` equal to the resulting string: `"#d#a#d#c#c#d#a#d#c#c#d#"`.
Rather than going through the entire example, we will focus, instead,
on the key steps:

1. start
2. when it encounters its first long palindrome `"dadccdad"` 
3. when it encounters its longest palindrome `"dccdadccd"`

For (1), `j = 0` and `P` is an array of length and capacity
equal to 23. For `i = 0`, since `i = j + P[j]`, we skip the block,
and begin by defining `k = pivot(0, 0)` which equals 0. 

In this case, `k - P[k]` equals 0, and therefore, equal to 
`j - P[j]` (the `else` condition applies). In this case, 
`findMaxPalindromeLen` returns 0, and nothing consequential
happens thereafter.

Going into (2), `j = 7`, `P = [0,1,0,3,0,1,0,1,0,...,0]` and
`i = 8`. In this case, `i == j + P[j]` and, skipping the first
`if`-block, we set `k = pivot(8, 7)` which equals `6`. From this,
we see that `k - P[k]` equals 6, which equals `j - P[j]`.

At this point, we start to find the maximal palindrome length
at `i` starting at position `9`. In this case, the length of
the maximal palindrome is 8. We set `P[8] = 8` and, since this
new palindrome extends beyond the one centered at 7, we set 
`j = 8`.

Finally, going into (3), we have `i = 13`, `j = 8` and 
`P = [0,1,0,3,0,1,0,1,8,1,0,1,0,0,...,0]`. We see that `i < j + P[j]`
and, once again, we skip the `if`-block. Setting `k = pivot(13, 8)` which
equals 3, we then have `k - P[k]` equal to 0, which equals to `j - P[j]`;
we are now executing the `else`-block.

At this point, we find the maximal palindrome length at 13 starting at
17; the maximal palindrome has length 9, and we set `P[13] = 9`. Since
`i + P[i] > j + P[j]`, we update `j = 13`.

## Linearity of Manacher's Algorithm

Looking at `FindPManacher`, it is not obvious that this function runs
in linear time. The interaction between the `for`-loop and `findMaxPalindromeLen`
might indicate that, in some pathological example, the algorithm runs
in quadratic time.

There are two ways that we would like to prove that `FindPManacher`, in fact,
has linear run-time. The first way involves a clever argument and no additional
machinery. The second way -- which we will cover in another entry -- involves
potential functions and amortized analysis. 

Before we present the first proof, we make the observation that the
only potential source of non-linearity of Manacher's is from comparing
characters in `findMaxPalindromeLen`; the other cases in the `for`-loop
in `FindPManacher` are constant time operations. Therefore, to demonstrate
that `FindPManacher` runs in linear time, it is enough to prove that
the total number of comparisons across all iterations is {{im}}O(n){{mi}}.
In fact, the first proof shows that the total number of comparisons is
bounded by {{im}}2n{{mi}}.

> The function `FindPManacher` runs in linear time with respect to 
the length of the input string `s`.

*Proof*: Fix the string {{im}}s{{mi}}, and consider the iterations
in `FindPManacher` on each character of {{im}}s{{mi}}. Let {{im}}C_i{{mi}}
denote the number of character comparisons that takes place during the 
{{im}}i^{\mathrm{th}}{{mi}} iteration. These character comparisons would
take place as a result of calling `findMaxPalindromeLen` when, for a given
centre, we compare right and left characters with respect to the center
to detect the size of the largest possible palindrome.

Recall that, as we iterate through each character, {{im}}j{{mi}} is tracking
the index of the center of the palindrome whose right boundary extends to
the right-most position in {{im}}s{{mi}}. That is, {{im}}j + P[j]{{mi}} is
the largest of all known palindromes at the start of the 
{{im}}i^{\mathrm{th}}{{mi}} iteration. Let {{im}}I_i{{mi}} denote
{{im}}2n - i - (j + P[j]){{mi}}.

The idea is to show that

1. {{im}}I_i{{mi}} is strictly monotonic, with {{im}}I_0 = 2n{{mi}} and
   {{im}}I_n = 1{{mi}}
2. {{im}}C_i \leq I_{i - 1} - I_i{{mi}}

With (1) and (2), we have that
{{dm}}\sum_i C_i \leq \sum_i (I_{i - 1} - I_{i}) = I_0 - I_n = 2n - 1{{md}}
Therefore, the number of comparisons are bounded by {{im}}2n{{mi}}. (As math 
and computer science treats indices differently, some inconsequential tidyness 
is sacrificed for the sake of bridging the two languages.) 

The intuition here is that the more you compare in 
the {{im}}i^{\mathrm{th}}{{mi}} iteration, the less you need to compare in
future iterations; you may need to access the same characters multiple times
-- for example in `aaaab` -- but total number of times you access 
the same characters is bounded by {{im}}n{{mi}}.

To show (1), note that {{im}}i{{mi}} increases strictly monotonically and
{{im}}j + P[j]{{mi}} only changes when {{im}}i + P[i] > j + P[j]{{mi}}
for some {{im}}i{{mi}}. 

The fact that {{im}}I_0 = 2n{{mi}} follows from initial values of {{im}}j{{mi}}
and {{im}}P[j]{{mi}}. When {{im}}i = n{{mi}}, {{im}}j + P[j] = n - 1{{mi}}. It
follows that {{im}}I_{n} = 1{{mi}}. 

To show (2), recall that during an iteration within `FindPManacher`, we do one 
of the following:

(a) we retrieve existing values from {{im}}P{{mi}} and do not make any 
  comparisons and just increment {{im}}i{{mi}}.

(b) we make some comparison but does not change the right-most boundary 
  {{im}}j + P[j]{{mi}}

(c) we make some comparisons and we do update the right-most boundary 
  {{im}}j + P[j]{{mi}}

In case (a), {{im}}I_{i - 1} - I_i = 1{{mi}} and {{im}}C_i = 0{{mi}}. In
this case, {{im}}C_i \leq I_{i - 1} - I_i{{mi}}. Good.

In case (b), {{im}}I_{i - 1} - I_i = 1{{mi}}. We claim that we made 
at most one comparisons. If we don't update {{im}}j + P[j]{{mi}}, this
means that for some {{im}}i > j{{mi}} and {{im}}P[i] = j + P[j] - i{{mi}},
and value returned by `findMaxPalindromeLen(s, i, j + P[j] + 1)` is
{{im}}j + P[j] - i{{mi}}.

Looking at the function `findMaxPalindromeLen`, this can only happen if either
{{im}}j + P[j] + 1 > len(s){{mi}} -- in which case, the number of comparison
is 0, the pivot of {{im}}j + P[j] + 1{{mi}} about {{im}}i{{mi}} is less than 0
-- impossible, or {{im}}s[j + P[j] + 1]{{mi}} is the only comparison made --
namely, we made a single comparison.

In all these possible cases, {{im}}1 = I_{i - 1} - I_i \geq C_i{{mi}}. Good.

Finally, in case (c), suppose {{im}}(i + P[i]) - (j + P[j]) = c{{mi}}. We want
to show that {{im}}c \geq C_i - 1{{mi}}. To see this, we know that the return values
of `findMaxPalindromeLen` is {{im}}P[i]{{mi}} by definition. This must mean that the
value of `right` at the end of the `for`-loop in 
`findMaxPalindrome(s, i, j + P[j] + 1)` is {{im}}i + P[i] + 1{{mi}}. Since `right`
is initialised at `j + P[j] + 1`, the number of iterations in the `for`-loop is
at most {{im}}i + P[i] - (j + P[j]) + 1{{mi}}, each of which is possibly a 
comparison. It follows that {{im}}c \geq C_i - 1{{mi}} and 
{{dm}}I_{n - 1} - I_n = i - (i - 1) + c \geq 1 + (C_i - 1) = C_i{{md}}
as desired.
{{im}}\blacksquare{{mi}}

Readers familiar with potential functions in amortized analysis will note 
that we actually defined a potential function {{im}}\Phi(i) = C_i{{mi}} 
and worked around the mechanics of analysing using potential functions.
However, we are satisfied with how little machinery we needed to present
to prove the linearity of Manacher's algorithm that we thought it is a
treat to include it without another few pages of the theory behind 
amortization analysis.

Before we end this section, let us note that, since it is not possible to 
find palindromes without at least scanning the string from beginning to 
end, the following is clear:

> Manacher's algorithm has optimal run-time complexity in resolving the maximal
palindrome question. {{im}}\blacksquare{{mi}}

## Acknowledgement

I want to thank the many Youtubers and authors who contributed to my
understanding of this algorith, all of whom are represented in the
references. 

The proof of linearity is my own. However, I apologise profusely
that ideas often overlap in shape and presentation, especially if they
are very wrong or very right.

## Reference

- [1] Hacker Rank - https://www.hackerrank.com/topics/manachers-algorithm
- [2] Tushar Roy's Manacher Walk-through - https://www.youtube.com/watch?v=V-sEwsca1ak
- [3] MIT OCW - 6.046 Lecture 5 - Amortization: Amortized Analysis - https://www.youtube.com/watch?v=3MpzavN3Mco
