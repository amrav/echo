#include <bits/stdc++.h>
using namespace std;

typedef long long int ll;

#include <sstream>

template <class T>
inline string to_string (const T& t)
{
    stringstream ss;
    ss << t;
    return ss.str();
}

int main() {

  cout << "Enter url: ";
  string url;
  cin >> url;
  int i, temp;
  ll final_no = 0;
  
  for(i = 0; i < (int) url.length(); i++) {
    if(url[i] >= 'A' && url[i] <= 'Z') temp = url[i] - 'A';
    else if(url[i] >= 'a' && url[i] <= 'z') temp = 26 + url[i] - 'a';
    else temp = 52 + url[i] - '0';
    temp += 1;
    final_no *= 63;
    final_no += temp;
    
  }
  //1<11 digits>
  string no_str = to_string(final_no);
  while((int)no_str.length() != 11) {
    no_str = "0" + no_str;
  }
  no_str = "1" + no_str;
  
  
  cout << no_str << endl;
  return 0;
}
  
